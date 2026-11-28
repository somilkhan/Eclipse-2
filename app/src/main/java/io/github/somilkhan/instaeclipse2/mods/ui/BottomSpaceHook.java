package io.github.somilkhan.instaeclipse2.mods.ui;

import android.app.Activity;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Removes the empty strip Instagram reserves under its bottom tab bar for the system navigation
 * bar (port of Piko's "Remove empty bottom space").
 *
 * <p>Instagram measures the system nav bar in a static {@code int (Activity, int, int, int)}
 * method that uses the strings "config_showNavigationBar" and "_hasNavigationBar_notFound"
 * (X.08be.A00 on 458.0.0.0.57). Its only caller is the window-insets listener, which stores the
 * result as the app-wide nav bar height. The method already returns 0 when the device has no nav
 * bar, so returning 0 here takes Instagram's own "no nav bar" path; nothing else is changed.
 * Takes effect after Instagram restarts.
 */
public class BottomSpaceHook {

    private static final String TAG = "(InstaEclipse | BottomSpace): ";

    public void install(DexKitBridge bridge) {
        try {
            Method target = null;
            int n = 0;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("int").usingStrings("config_showNavigationBar", "_hasNavigationBar_notFound")))) {
                Method m = md.getMethodInstance(io.github.somilkhan.instaeclipse2.Xposed.Module.hostClassLoader);
                Class<?>[] p = m.getParameterTypes();
                if (p.length < 1 || !Activity.class.isAssignableFrom(p[0])) continue;
                target = m;
                n++;
            }
            // Fail closed: zeroing the wrong int method could break layout anywhere in the app.
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 nav bar size method, found " + n);
                return;
            }
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (FeatureFlags.removeEmptyBottomSpace) param.setResult(0);
                }
            });
            ModuleLog.line(TAG + "✅ Hooked " + target.getDeclaringClass().getName() + "." + target.getName());
            FeatureStatusTracker.setHooked("RemoveEmptyBottomSpace");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }
}
