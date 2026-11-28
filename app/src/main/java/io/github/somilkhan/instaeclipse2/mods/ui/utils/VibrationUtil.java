package io.github.somilkhan.instaeclipse2.mods.ui.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

public class VibrationUtil {
    @SuppressLint("ObsoleteSdkInt")
    public static void vibrate(Context context) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50); // For older Android versions
                }
            }
        } catch (Exception e) {
            ModuleLog.line("InstaEclipse (vibrate error): " + e.getMessage());
        }
    }
}
