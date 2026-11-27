package io.github.somilkhan.instaeclipse2.utils.crash;

import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.BuildConfig;
import io.github.somilkhan.instaeclipse2.utils.log.Logging;

/**
 * Runs inside the hooked Instagram process. Writes a crash report for:
 * <ul>
 *   <li>Java crashes, live, from {@code Thread.dispatchUncaughtException} (before Instagram's
 *       own handler, so a handler that kills the process itself cannot skip it);</li>
 *   <li>native crashes and ANRs, on the next start, from {@link ApplicationExitInfo} (API 30+),
 *       because no Java code runs when the process dies from a signal or is killed for an ANR.</li>
 * </ul>
 * Each report is first written to Instagram's own files dir (pending), then handed to the companion
 * ({@link CrashReportProvider}), which saves it into the folder the user picked. A report the
 * companion did not accept stays pending and is retried on the next start.
 */
public final class CrashReporter {
    private static final String PENDING_DIR = "eclipse2_crash_pending";
    private static final String PREFS = "eclipse2_crash";
    private static final String KEY_LAST_EXIT = "lastExitTs";
    private static final String KEY_SAVED = "savedSinceLastStart";
    private static final int LOG_TAIL_LINES = 400;
    private static final int MAX_TRACE_BYTES = 256 * 1024; // == CrashReportProvider.MAX_RAW_BYTES
    private static final long DELIVERY_WAIT_MS = 4000;

    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean REPORTING = new AtomicBoolean();
    private static volatile Context app;
    private static volatile String packageName;

    private CrashReporter() { }

    /** Call as early as possible in the main Instagram process. */
    public static void install(String pkg) {
        packageName = pkg;
        if (!INSTALLED.compareAndSet(false, true)) return;
        try {
            XposedBridge.hookAllMethods(Thread.class, "dispatchUncaughtException", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args.length > 0 && p.args[0] instanceof Throwable t)
                        onUncaught((Thread) p.thisObject, t);
                }
            });
            return;
        } catch (Throwable ignored) { }
        // Fallback: wrap whatever default handler exists now.
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            onUncaught(thread, error);
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    /** Application is attached: retry pending reports and look for native crashes / ANRs. */
    public static void onAppReady(Context context) {
        if (context == null) return;
        app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        if (packageName == null) packageName = context.getPackageName();
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(6000); // keep it off Instagram's startup path
                int saved = flushPending(app);
                saved += scanExitReasons(app);
                SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                saved += prefs.getInt(KEY_SAVED, 0);
                prefs.edit().putInt(KEY_SAVED, 0).apply();
                if (saved > 0) notifySaved(app, saved);
            } catch (Throwable ignored) { }
        }, "Eclipse2-CrashScan");
        worker.setDaemon(true);
        worker.start();
    }

    // ── Java crashes ─────────────────────────────────────────────────────────

    private static void onUncaught(Thread thread, Throwable error) {
        if (!REPORTING.compareAndSet(false, true)) return; // first crash only
        // Everything runs on a helper thread with a bounded wait: the crashing thread may hold
        // locks (even the log's), and the companion may need a moment to start.
        Thread helper = new Thread(() -> {
            try {
                Context ctx = app != null ? app : AndroidAppHelper.currentApplication();
                if (ctx == null) return;
                String name = fileName(ctx, "java");
                StringBuilder r = header(ctx, "Java crash (uncaught exception)", System.currentTimeMillis());
                r.append("Thread: ").append(thread == null ? "?" : thread.getName()).append('\n');
                r.append("Exception: ").append(error.getClass().getName()).append('\n');
                if (error.getMessage() != null) r.append("Message: ").append(error.getMessage()).append('\n');
                r.append("Module frames: ").append(mentionsModule(error) ? "yes" : "no").append("\n\n");
                r.append("── Stack trace ──\n");
                StringWriter sw = new StringWriter();
                error.printStackTrace(new PrintWriter(sw));
                r.append(sw).append('\n');
                appendLogTail(r);
                String text = r.toString();
                File pending = savePending(ctx, name, text);
                int result = deliver(ctx, name, text, null);
                if (result != FAILED && pending != null) pending.delete();
                if (result == SAVED) {
                    SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    prefs.edit().putInt(KEY_SAVED, prefs.getInt(KEY_SAVED, 0) + 1).commit();
                }
            } catch (Throwable ignored) { }
        }, "Eclipse2-CrashReport");
        helper.setDaemon(true);
        helper.start();
        try { helper.join(DELIVERY_WAIT_MS); } catch (InterruptedException ignored) { }
    }

    private static boolean mentionsModule(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause())
            for (StackTraceElement e : t.getStackTrace())
                if (e.getClassName().startsWith(BuildConfig.APPLICATION_ID)) return true;
        return false;
    }

    // ── Native crashes and ANRs (next start) ─────────────────────────────────

    private static int scanExitReasons(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_EXIT, -1);
        ActivityManager am = ctx.getSystemService(ActivityManager.class);
        if (am == null) return 0;
        List<ApplicationExitInfo> exits;
        try { exits = am.getHistoricalProcessExitReasons(ctx.getPackageName(), 0, 16); }
        catch (Throwable t) { return 0; }
        long newest = last;
        for (ApplicationExitInfo e : exits) newest = Math.max(newest, e.getTimestamp());
        prefs.edit().putLong(KEY_LAST_EXIT, newest).apply();
        if (last < 0) return 0; // first run with this feature: baseline only, no old reports
        int saved = 0;
        for (ApplicationExitInfo e : exits) {
            if (e.getTimestamp() <= last) continue;
            if (!ctx.getPackageName().equals(e.getProcessName())) continue; // main process only
            int reason = e.getReason();
            if (reason != ApplicationExitInfo.REASON_CRASH_NATIVE && reason != ApplicationExitInfo.REASON_ANR) continue;
            try {
                boolean nativeCrash = reason == ApplicationExitInfo.REASON_CRASH_NATIVE;
                String name = fileName(ctx, nativeCrash ? "native" : "anr", e.getTimestamp());
                StringBuilder r = header(ctx, nativeCrash ? "Native crash (signal)" : "ANR (app not responding)", e.getTimestamp());
                r.append("Detected: on the next Instagram start\n");
                r.append("PID: ").append(e.getPid()).append('\n');
                r.append("Status: ").append(e.getStatus()).append(nativeCrash ? " (signal number)" : "").append('\n');
                if (e.getDescription() != null) r.append("Description: ").append(e.getDescription()).append('\n');
                r.append("Memory: pss=").append(e.getPss()).append(" kB, rss=").append(e.getRss()).append(" kB\n\n");
                byte[] trace = readTrace(e);
                byte[] raw = null;
                if (trace != null && trace.length > 0) {
                    if (nativeCrash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // API 31+: the tombstone comes as protobuf. Decode the useful part to text
                        // and keep the raw file alongside for `pbtombstone`.
                        r.append(TombstoneText.decode(trace)).append('\n');
                        raw = trace;
                    } else {
                        r.append("── Trace ──\n").append(new String(trace, StandardCharsets.UTF_8)).append('\n');
                    }
                } else {
                    r.append("(The system kept no trace for this exit.)\n\n");
                }
                appendLogTail(r);
                String text = r.toString();
                File pending = savePending(ctx, name, text);
                int result = deliver(ctx, name, text, raw);
                if (result != FAILED && pending != null) pending.delete();
                if (result == SAVED) saved++;
            } catch (Throwable ignored) { }
        }
        return saved;
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private static byte[] readTrace(ApplicationExitInfo e) {
        try (InputStream in = e.getTraceInputStream()) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                int room = MAX_TRACE_BYTES - out.size();
                if (room <= 0) break;
                out.write(buf, 0, Math.min(n, room));
            }
            return out.toByteArray();
        } catch (Throwable t) { return null; }
    }

    // ── Report pieces ────────────────────────────────────────────────────────

    private static StringBuilder header(Context ctx, String kind, long when) {
        StringBuilder r = new StringBuilder(16 * 1024);
        r.append("Eclipse 2 crash report\n======================\n");
        r.append("Type: ").append(kind).append('\n');
        r.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date(when))).append('\n');
        r.append("App: ").append(ctx.getPackageName());
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            r.append(' ').append(pi.versionName).append(" (").append(pi.getLongVersionCode()).append(')');
        } catch (Throwable ignored) { }
        r.append('\n');
        r.append("Module: Eclipse 2 ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n");
        r.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" (").append(Build.DEVICE).append(")\n");
        r.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        r.append("ABI: ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?").append('\n');
        r.append("Build: ").append(Build.FINGERPRINT).append("\n\n");
        return r;
    }

    private static void appendLogTail(StringBuilder r) {
        try {
            Logging.flush();
            String log = Logging.getSnapshot();
            if (log == null || log.isEmpty()) return;
            String[] lines = log.split("\n");
            int from = Math.max(0, lines.length - LOG_TAIL_LINES);
            r.append("── Eclipse 2 log (last ").append(lines.length - from).append(" lines) ──\n");
            for (int i = from; i < lines.length; i++) r.append(lines[i]).append('\n');
        } catch (Throwable ignored) { }
    }

    private static String fileName(Context ctx, String kind) { return fileName(ctx, kind, System.currentTimeMillis()); }

    private static String fileName(Context ctx, String kind, long when) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date(when));
        return "crash_" + ctx.getPackageName() + "_" + stamp + "_" + kind;
    }

    // ── Pending store + delivery ─────────────────────────────────────────────

    private static File savePending(Context ctx, String name, String text) {
        try {
            File dir = new File(ctx.getFilesDir(), PENDING_DIR);
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            File f = new File(dir, name + ".txt");
            try (FileOutputStream out = new FileOutputStream(f)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
            return f;
        } catch (Throwable t) { return null; }
    }

    private static int flushPending(Context ctx) {
        File dir = new File(ctx.getFilesDir(), PENDING_DIR);
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int saved = 0;
        for (File f : files) {
            String n = f.getName();
            if (!n.endsWith(".txt")) { f.delete(); continue; }
            if (System.currentTimeMillis() - f.lastModified() > 7L * 24 * 3600 * 1000) { f.delete(); continue; }
            try (FileInputStream in = new FileInputStream(f)) {
                if (f.length() > 4L * CrashReportProvider.MAX_TEXT_CHARS) { f.delete(); continue; }
                byte[] b = new byte[(int) f.length()];
                int off = 0, r;
                while (off < b.length && (r = in.read(b, off, b.length - off)) > 0) off += r;
                int result = deliver(ctx, n.substring(0, n.length() - 4), new String(b, 0, off, StandardCharsets.UTF_8), null);
                if (result != FAILED) f.delete();
                if (result == SAVED) saved++;
            } catch (Throwable ignored) { }
        }
        return saved;
    }

    private static final int FAILED = 0, SAVED = 1, DISABLED = 2;

    /** SAVED, DISABLED (crash reports switched off in the companion) or FAILED (keep pending). */
    private static int deliver(Context ctx, String name, String text, byte[] raw) {
        try {
            Bundle b = new Bundle();
            b.putString(CrashReportProvider.EXTRA_NAME, name);
            if (text.length() > CrashReportProvider.MAX_TEXT_CHARS) {
                // Keep the head (header, signal, stack) and the end of the log tail.
                int head = CrashReportProvider.MAX_TEXT_CHARS * 2 / 3;
                int tail = CrashReportProvider.MAX_TEXT_CHARS - head - 64;
                text = text.substring(0, head) + "\n… (truncated) …\n" + text.substring(text.length() - tail);
            }
            b.putString(CrashReportProvider.EXTRA_TEXT, text);
            if (raw != null && raw.length <= CrashReportProvider.MAX_RAW_BYTES) b.putByteArray(CrashReportProvider.EXTRA_RAW, raw);
            Bundle r = ctx.getContentResolver().call(CrashReportProvider.URI, CrashReportProvider.METHOD_REPORT, null, b);
            if (r == null) return FAILED;
            if (r.getBoolean(CrashReportProvider.RESULT_SAVED)) return SAVED;
            return r.getBoolean(CrashReportProvider.RESULT_DISABLED) ? DISABLED : FAILED;
        } catch (Throwable t) {
            return FAILED; // companion missing or too old: stays pending
        }
    }

    private static void notifySaved(Context ctx, int count) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                android.widget.Toast.makeText(ctx, count == 1
                        ? "Eclipse 2 saved a crash report from the last session"
                        : "Eclipse 2 saved " + count + " crash reports", android.widget.Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) { }
        });
    }
}
