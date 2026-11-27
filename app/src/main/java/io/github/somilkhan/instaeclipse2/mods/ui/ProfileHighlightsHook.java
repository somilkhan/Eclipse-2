package io.github.somilkhan.instaeclipse2.mods.ui;

import android.annotation.SuppressLint;
import android.view.View;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Hides the story highlights row on profiles (port of Piko's "Disable highlights").
 *
 * <p>The profile header is bound by {@code bindView(int, View, Object, Object)V}, which logs the
 * systrace section "ProfileHeaderBinderGroup.bindView" (X.0JEo on 458.0.0.0.57). It is called
 * once per header part; the part to hide is the view with id {@code highlights_tray}.
 *
 * <p>Instagram itself sets the tray to GONE when a profile has no highlights. So before each bind
 * the view gets back the visibility Instagram last chose, and after the bind that value is saved
 * and the tray is hidden. This way our GONE is never read as "no highlights" by the next bind,
 * and turning the switch off shows the real state on the next bind.
 */
public class ProfileHighlightsHook {

    private static final String TAG = "(InstaEclipse | Highlights): ";
    private static final Map<View, Integer> NATIVE_VISIBILITY =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static int trayId = -1;

    public void install(DexKitBridge bridge) {
        try {
            int n = 0;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .name("bindView").returnType("void").paramCount(4)
                    .usingStrings("ProfileHeaderBinderGroup.bindView")))) {
                Method m = md.getMethodInstance(io.github.somilkhan.instaeclipse2.Xposed.Module.hostClassLoader);
                Class<?>[] p = m.getParameterTypes();
                if (p[0] != int.class || p[1] != View.class) continue;
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                        View v = (View) param.args[1];
                        if (!isTray(v)) return;
                        Integer nativeVisibility = NATIVE_VISIBILITY.get(v);
                        if (nativeVisibility != null) v.setVisibility(nativeVisibility);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View v = (View) param.args[1];
                        if (!isTray(v)) return;
                        NATIVE_VISIBILITY.put(v, v.getVisibility());
                        if (FeatureFlags.disableHighlights) v.setVisibility(View.GONE);
                    }
                });
                n++;
                ModuleLog.line(TAG + "✅ Hooked " + md.getClassName() + "." + md.getName());
            }
            if (n > 0) FeatureStatusTracker.setHooked("DisableHighlights");
            else ModuleLog.line(TAG + "❌ ProfileHeaderBinderGroup.bindView not found");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    @SuppressLint("DiscouragedApi")
    private static boolean isTray(View v) {
        if (v == null) return false;
        int id = trayId;
        if (id == -1) {
            try {
                id = v.getResources().getIdentifier("highlights_tray", "id", v.getContext().getPackageName());
            } catch (Throwable t) {
                id = 0;
            }
            trayId = id;
        }
        return id != 0 && v.getId() == id;
    }
}
