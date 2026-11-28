package io.github.somilkhan.instaeclipse2.utils.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.mods.ui.UIHookManager;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Launches one of InstaEclipse's own Activities (e.g. LocationPickerActivity) from code
 * running inside Instagram's hooked process. Uses an authenticated one-shot capability for the private target Activity.
 */
public final class ModuleActivityLauncher {

    private ModuleActivityLauncher() {}

    public static boolean launch(Context context, String activityClassName, Bundle extras) {
        Activity activity = UIHookManager.getCurrentActivity();
        if (activity == null || activity.isFinishing()) {
            ModuleLog.line("(InstaEclipse | Launcher): no active Instagram activity");
            return false;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(CommonUtils.MY_PACKAGE_NAME, activityClassName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (extras != null) intent.putExtras(extras);
            return io.github.somilkhan.instaeclipse2.utils.ipc.CompanionBridge.launchActivity(activity, intent);
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Launcher): failed " + activityClassName + " -> " + t.getMessage());
            return false;
        }
    }
}
