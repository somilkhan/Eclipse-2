package io.github.somilkhan.instaeclipse2.utils.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.List;

public class CommonUtils {
    public static final String IG_PACKAGE_NAME = "com.instagram.android";
    public static final String MY_PACKAGE_NAME = "io.github.somilkhan.instaeclipse2";

    // In-app log viewer IPC
    public static final String ACTION_REQUEST_LOGS = "io.github.somilkhan.instaeclipse2.ACTION_REQUEST_LOGS";
    public static final String ACTION_LOGS_REPLY = "io.github.somilkhan.instaeclipse2.ACTION_LOGS_REPLY";
    public static final String ACTION_CLEAR_LOGS = "io.github.somilkhan.instaeclipse2.ACTION_CLEAR_LOGS";
    public static final String EXTRA_LOG_TEXT = "log_text";
    public static final String EXTRA_LOG_SOURCE = "source_package";
    public static final String EXTRA_LOG_ERROR = "error";

    /** All Instagram packages this module hooks into. */
    public static final List<String> SUPPORTED_PACKAGES = Arrays.asList(
            "com.instagram.android",
            "com.instagold.android",
            "com.instaflux.app",
            "com.myinsta.android",
            "cc.honista.app",
            "com.instaprime.android",
            "com.instafel.android",   // Instafel clone build (unclone uses com.instagram.android)
            "com.instaalpha.android", // InstaAlpha clone build (unclone uses com.instagram.android)
            "com.instadm.android",
            "com.dfistagram.android",
            "com.Instander.android",
            "com.aero.instagram",
            "com.instapro.android",
            "com.instaflow.android",
            "com.instagram1.android",
            "com.instagram2.android",
            "com.instagramclone.android",
            "com.instaclone.android"
    );

    /**
     * Returns a human-readable variant label for a package name.
     * "com.instagram.android" → "Official"
     * "com.instaflow.android" → "Instaflow"
     * "cc.honista.app"        → "Honista"
     */
    public static String getVariantLabel(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "";
        if (IG_PACKAGE_NAME.equals(packageName)) return "Official";
        String[] parts = packageName.split("\\.");
        // Use the most descriptive segment: skip generic TLDs and short segments
        String best = parts.length >= 2 ? parts[1] : packageName;
        // If second segment is very short or generic, try third
        if (best.length() <= 2 && parts.length >= 3) best = parts[2];
        if (best.isEmpty()) return packageName; // e.g. "a..b": charAt(0) used to throw
        // Locale.ROOT: with a Turkish device locale "INSTANDER".toLowerCase() gives "ınstander".
        return best.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                + best.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Companion side of CompanionSync: a setting sent to Instagram is also written, with a
     * timestamp ("__ts_<key>"), to the companion's instaeclipse_cache prefs that Instagram reads
     * through XSharedPreferences. Instagram applies it on its next start or activity resume when
     * the live broadcast didn't reach it (not running, or frozen in the background).
     */
    /**
     * The companion's instaeclipse_cache prefs are world-readable so the hooked Instagram process
     * can read them through the legacy XSharedPreferences API on a cold start. Do not opt into
     * LSPosed's New XSharedPreferences extension here: it is deprecated and scheduled for removal.
     * On Android versions that reject MODE_WORLD_READABLE, fall back to MODE_PRIVATE; live setting
     * updates still use the companion-to-Instagram broadcast bridge.
     * Every companion-side open must go through here: a process keeps the first mode it used.
     */
    @SuppressWarnings("deprecation")
    public static android.content.SharedPreferences companionCache(Context context) {
        try {
            return context.getSharedPreferences("instaeclipse_cache", Context.MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            return context.getSharedPreferences("instaeclipse_cache", Context.MODE_PRIVATE);
        }
    }

    private static void stampSetting(Context context, Intent intent) {
        String action = intent.getAction();
        String key = intent.getStringExtra("key");
        if (action == null || key == null || !action.startsWith("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF")) return;
        try {
            long now = System.currentTimeMillis();
            android.content.SharedPreferences.Editor e =
                    companionCache(context).edit();
            Object v = intent.getExtras() != null ? intent.getExtras().get("value") : null;
            if (v instanceof Boolean) e.putBoolean(key, (Boolean) v);
            else if (v instanceof Integer) e.putInt(key, (Integer) v);
            else if (v instanceof String) e.putString(key, (String) v);
            e.putLong("__ts_" + key, now).commit();
            intent.putExtra("ts", now);
        } catch (Throwable ignored) {}
    }

    /** Sends commands only to an explicitly trusted installed host signing identity. */
    public static void broadcastToInstagram(Context context, Intent intent) {
        stampSetting(context, intent);
        PackageManager pm = context.getPackageManager();
        for (String pkg : SUPPORTED_PACKAGES) {
            try {
                if (intent.getPackage() != null && !pkg.equals(intent.getPackage())) continue;
                if (!io.github.somilkhan.instaeclipse2.utils.ipc.CompanionBridge.isTrustedHost(context, pkg)) continue;
                pm.getPackageInfo(pkg, 0);
                Intent targeted = new Intent(intent);
                targeted.setPackage(pkg);
                context.sendBroadcast(targeted);
            } catch (Throwable ignored) {}
        }
    }

    /*
    Dev Purposes
    public static final String USER_SESSION_CLASS = "com.instagram.common.session.UserSession";
    */
}
