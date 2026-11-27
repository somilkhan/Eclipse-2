package io.github.somilkhan.instaeclipse2.utils.crash;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.regex.Pattern;

import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;

/**
 * Lives in the companion app. A hooked Instagram process hands it a finished crash report
 * (see {@link CrashReporter}) and the companion writes it into the folder the user picked
 * ({@code crashReportUri}), else the download folder ({@code downloaderCustomUri}), else
 * {@code Download/Eclipse 2/Crash reports}.
 *
 * <p>Only supported Instagram packages may call it, by package name like {@code LogMirrorProvider}:
 * the worst a caller can do is drop a size-capped text file into the crash folder.
 */
public class CrashReportProvider extends ContentProvider {

    public static final String AUTHORITY = CommonUtils.MY_PACKAGE_NAME + ".crashreport";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);
    public static final String METHOD_REPORT = "report";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_RAW = "raw";
    public static final String RESULT_SAVED = "saved";
    public static final String RESULT_DISABLED = "disabled";
    public static final String RESULT_PATH = "path";
    /** Report text cap in chars. Bundles carry strings as UTF-16 and a binder call is limited
     *  to ~1 MB, so text (≤ 320 KB on the wire) + raw tombstone (≤ 256 KB) stays well under it. */
    public static final int MAX_TEXT_CHARS = 160_000;
    public static final int MAX_RAW_BYTES = 256 * 1024;

    public static final String PREF_ENABLED = "crashReports";
    public static final String PREF_URI = "crashReportUri";
    public static final String PREF_PATH = "crashReportPath";
    public static final String SUBFOLDER = "Crash reports";

    private static final Pattern NAME = Pattern.compile("crash_[A-Za-z0-9._-]{1,120}");
    private static final int MAX_PER_HOUR = 30;
    private static final ArrayDeque<Long> recent = new ArrayDeque<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Bundle out = new Bundle();
        Context ctx = getContext();
        if (ctx == null || !METHOD_REPORT.equals(method) || extras == null) return out;
        if (callerPackage(ctx) == null) throw new SecurityException("caller is not a supported Instagram package");

        SharedPreferences prefs = CommonUtils.companionCache(ctx);
        if (!prefs.getBoolean(PREF_ENABLED, true)) {
            out.putBoolean(RESULT_DISABLED, true);
            return out;
        }

        String name = extras.getString(EXTRA_NAME);
        String text = extras.getString(EXTRA_TEXT);
        byte[] raw = extras.getByteArray(EXTRA_RAW);
        if (name == null || !NAME.matcher(name).matches() || text == null) return out;
        if (text.length() > MAX_TEXT_CHARS) text = text.substring(0, MAX_TEXT_CHARS) + "\n… (truncated)\n";
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        if (raw != null && raw.length > MAX_RAW_BYTES) raw = null;
        if (!allow()) return out;

        long token = Binder.clearCallingIdentity();
        try {
            String path = save(ctx, prefs, name, body, raw);
            if (path != null) {
                out.putBoolean(RESULT_SAVED, true);
                out.putString(RESULT_PATH, path);
            }
        } catch (Throwable ignored) {
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        return out;
    }

    private static synchronized boolean allow() {
        long now = System.currentTimeMillis();
        while (!recent.isEmpty() && now - recent.peekFirst() > 3_600_000L) recent.pollFirst();
        if (recent.size() >= MAX_PER_HOUR) return false;
        recent.addLast(now);
        return true;
    }

    @Nullable
    private static String callerPackage(Context ctx) {
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid()) return CommonUtils.IG_PACKAGE_NAME; // local testing only
        String[] pkgs = ctx.getPackageManager().getPackagesForUid(uid);
        if (pkgs == null) return null;
        for (String p : pkgs) if (CommonUtils.SUPPORTED_PACKAGES.contains(p)) return p;
        return null;
    }

    @Nullable
    private static String save(Context ctx, SharedPreferences prefs, String name, byte[] body, @Nullable byte[] raw) {
        for (String key : new String[]{PREF_URI, "downloaderCustomUri"}) {
            String tree = prefs.getString(key, null);
            if (tree == null || tree.isEmpty()) continue;
            String path = saveToTree(ctx, Uri.parse(tree), name, body, raw);
            if (path != null) return path;
        }
        return saveToDownloads(ctx, name, body, raw);
    }

    @Nullable
    private static String saveToTree(Context ctx, Uri tree, String name, byte[] body, @Nullable byte[] raw) {
        try {
            DocumentFile root = DocumentFile.fromTreeUri(ctx, tree);
            if (root == null || !root.canWrite()) return null;
            DocumentFile dir = root.findFile(SUBFOLDER);
            if (dir == null || !dir.isDirectory()) dir = root.createDirectory(SUBFOLDER);
            if (dir == null) return null;
            ContentResolver cr = ctx.getContentResolver();
            DocumentFile txt = dir.createFile("text/plain", name + ".txt");
            if (txt == null || !write(cr, txt.getUri(), body)) return null;
            if (raw != null) {
                DocumentFile pb = dir.createFile("application/octet-stream", name + ".tombstone.pb");
                if (pb != null) write(cr, pb.getUri(), raw);
            }
            return SUBFOLDER + "/" + name + ".txt";
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static String saveToDownloads(Context ctx, String name, byte[] body, @Nullable byte[] raw) {
        String rel = Environment.DIRECTORY_DOWNLOADS + "/Eclipse 2/" + SUBFOLDER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver cr = ctx.getContentResolver();
            if (!insertDownload(cr, rel, name + ".txt", "text/plain", body)) return null;
            if (raw != null) insertDownload(cr, rel, name + ".tombstone.pb", "application/octet-stream", raw);
            return rel + "/" + name + ".txt";
        }
        // API 28: no scoped Downloads insert without storage permission; keep it in the companion.
        File dir = new File(ctx.getExternalFilesDir(null) != null ? ctx.getExternalFilesDir(null) : ctx.getFilesDir(), SUBFOLDER);
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        try (OutputStream os = new FileOutputStream(new File(dir, name + ".txt"))) {
            os.write(body);
        } catch (Throwable t) {
            return null;
        }
        if (raw != null) {
            try (OutputStream os = new FileOutputStream(new File(dir, name + ".tombstone.pb"))) {
                os.write(raw);
            } catch (Throwable ignored) { }
        }
        return new File(dir, name + ".txt").getAbsolutePath();
    }

    private static boolean insertDownload(ContentResolver cr, String rel, String file, String mime, byte[] data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        Uri uri = null;
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, file);
            cv.put(MediaStore.Downloads.MIME_TYPE, mime);
            cv.put(MediaStore.Downloads.RELATIVE_PATH, rel);
            cv.put(MediaStore.Downloads.IS_PENDING, 1);
            uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null || !write(cr, uri, data)) {
                if (uri != null) cr.delete(uri, null, null);
                return false;
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            cr.update(uri, done, null, null);
            return true;
        } catch (Throwable t) {
            try { if (uri != null) cr.delete(uri, null, null); } catch (Throwable ignored) { }
            return false;
        }
    }

    private static boolean write(ContentResolver cr, Uri uri, byte[] data) {
        try (OutputStream os = cr.openOutputStream(uri, "w")) {
            if (os == null) return false;
            os.write(data);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
