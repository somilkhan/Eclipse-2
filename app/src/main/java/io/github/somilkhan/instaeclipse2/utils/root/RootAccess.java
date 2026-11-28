package io.github.somilkhan.instaeclipse2.utils.root;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Optional root support for the companion app (off by default).
 *
 * <p>The "Use root (if available)" switch lives in the companion's own private prefs
 * ({@link #PREFS}); it is never synced to Instagram and is excluded from backups, so root is
 * always an explicit choice on this device. Root work runs on {@link #executor()}, never on the
 * main thread. See {@link RootCommands} for what may run and {@link RootActions} for the actions.
 */
public final class RootAccess {

    public static final String PREFS = "eclipse2_root";
    public static final String KEY_ENABLED = "useRoot";

    private static volatile RootSession session;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ie-root");
        t.setDaemon(true);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RootAccess() {}

    public static RootSession session(Context context) {
        RootSession s = session;
        if (s != null) return s;
        synchronized (RootAccess.class) {
            if (session == null) {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                session = new RootSession(new SuShellRunner(),
                        () -> prefs.getBoolean(KEY_ENABLED, false),
                        ModuleLog::line,
                        RootAccess::requireBackgroundThread);
            }
            return session;
        }
    }

    public static boolean isEnabled(Context context) {
        return session(context).isEnabled();
    }

    public static void setEnabled(Context context, boolean on) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, on).apply();
        session(context).reset();
        ModuleLog.line("(Root) \"Use root\" switched " + (on ? "on" : "off"));
    }

    /** One thread: root commands never overlap (a second prompt while one is showing is denied). */
    public static ExecutorService executor() {
        return EXECUTOR;
    }

    /** Re-checks off the main thread and delivers the status on the main thread. */
    public static void checkAsync(Context context, Consumer<RootStatus> onMain) {
        RootSession s = session(context);
        EXECUTOR.execute(() -> {
            s.reset();
            RootStatus st = s.check();
            MAIN.post(() -> onMain.accept(st));
        });
    }

    /** Runs {@code work} on the root thread and hands its result to {@code onMain}. */
    public static <T> void runAsync(java.util.function.Supplier<T> work, Consumer<T> onMain) {
        EXECUTOR.execute(() -> {
            T result;
            try {
                result = work.get();
            } catch (RuntimeException e) {
                ModuleLog.line("(Root) action crashed", e);
                result = null;
            }
            T r = result;
            MAIN.post(() -> onMain.accept(r));
        });
    }

    private static void requireBackgroundThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("root command on the main thread");
        }
    }

    /** User id of this app, for {@code am --user} and {@code /data/user/<id>}. */
    public static int userId() {
        return RootCommands.userIdOf(android.os.Process.myUid());
    }
}
