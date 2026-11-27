package io.github.somilkhan.instaeclipse2.Xposed;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.File;

import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Detects the original InstaEclipse module (or an older fork) running in the same Instagram
 * process. Both modules hook the same methods, so every feature runs twice: duplicate menu rows,
 * double DexKit scans at startup (slow cold start) and failures from the older hook code.
 *
 * <p>Other LSPosed modules live in their own class loaders, so they can't be looked up directly.
 * Their logger, however, writes {@code instaeclipse_module.log} in Instagram's files dir. If that
 * file is touched after this process started, an old module is active alongside us.
 */
final class LegacyModuleCheck {

    static final String LEGACY_LOG = "instaeclipse_module.log";
    private static final long CHECK_DELAY_MS = 20_000;

    private LegacyModuleCheck() {}

    static void schedule(Context context) {
        final File legacy = new File(context.getFilesDir(), LEGACY_LOG);
        // Wall-clock time this process started (elapsedRealtime is monotonic; convert).
        final long processStart = System.currentTimeMillis()
                - (SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (isActive(legacy.exists(), legacy.lastModified(), processStart)) {
                    ModuleLog.line("(Eclipse 2) ⚠️ Another InstaEclipse module is active in this Instagram "
                            + "process (it keeps writing " + LEGACY_LOG + "). Both modules hook the same "
                            + "methods, which duplicates features, slows startup and adds errors from the "
                            + "old hooks. Disable the old module in LSPosed, keep only Eclipse 2, then "
                            + "force-stop Instagram.");
                }
            } catch (Throwable ignored) {
            }
        }, CHECK_DELAY_MS);
    }

    /** Pure decision, unit-tested: old log written after this process started. */
    static boolean isActive(boolean exists, long lastModified, long processStartMillis) {
        // 2s slack for coarse file timestamps.
        return exists && lastModified > 0 && lastModified >= processStartMillis - 2_000;
    }
}
