package io.github.somilkhan.instaeclipse2.mods.piko;

import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Plays a chosen (or random) custom like animation on every post (Piko's "Custom like
 * animation").
 *
 * <p>458.0.0.0.57: {@code com.instagram.ui.mediaactions.LikeActionView} is configured per post in
 * A00(UserSession, renderEnum). With a non-default render value and a server gate on, it calls its
 * own {@code setUpCustomLikesAnimation(renderEnum)} and resets the view's transform; otherwise it
 * falls back to the plain heart. We do exactly what the custom branch does with our pick, without
 * the server gate. The render enum's constant names (RINGS_LIKE_*, ANYWAY_LIKE_ACTIVATION) aren't
 * obfuscated, so no DexKit search is needed: both methods are found from the view class by shape.
 */
public class LikeAnimationHook {

    private static final String TAG = "(InstaEclipse | LikeAnimation): ";
    private static final Random RANDOM = new Random();

    public void install() {
        try {
            Class<?> view = Class.forName("com.instagram.ui.mediaactions.LikeActionView", false, Module.hostClassLoader);
            Method setUp = null;
            for (Method m : view.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ("setUpCustomLikesAnimation".equals(m.getName()) && p.length == 1 && p[0].isEnum()) setUp = m;
            }
            if (setUp == null) {
                ModuleLog.line(TAG + "❌ setUpCustomLikesAnimation(enum) not found");
                return;
            }
            Class<?> render = setUp.getParameterTypes()[0];
            Method configure = null;
            int n = 0;
            for (Method m : view.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() != void.class || p.length != 2) continue;
                if (p[1] != render || !"com.instagram.common.session.UserSession".equals(p[0].getName())) continue;
                configure = m;
                n++;
            }
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 configure(UserSession, render), found " + n);
                return;
            }
            Map<String, Object> byName = new HashMap<>();
            List<String> names = new ArrayList<>();
            for (Object c : render.getEnumConstants()) {
                String name = ((Enum<?>) c).name();
                byName.put(name, c);
                names.add(name);
            }
            if (LikeAnimations.selectable(names).isEmpty()) {
                ModuleLog.line(TAG + "❌ render enum has no like animations");
                return;
            }
            final Method setUpCustom = setUp;
            setUpCustom.setAccessible(true);
            XposedBridge.hookMethod(configure, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.customLikeAnimation) return;
                    String pick = LikeAnimations.pick(FeatureFlags.likeAnimation, names, RANDOM);
                    Object value = pick != null ? byName.get(pick) : null;
                    if (value == null) return;
                    try {
                        setUpCustom.invoke(param.thisObject, value);
                        View v = (View) param.thisObject;
                        // Same reset Instagram's custom branch does before the animation plays.
                        v.setScaleX(0f);
                        v.setScaleY(0f);
                        v.setAlpha(1f);
                        v.setRotation(0f);
                        v.setTranslationX(0f);
                        v.setTranslationY(0f);
                        param.setResult(null);
                    } catch (Throwable t) {
                        ModuleLog.line(TAG + "⚠️ " + (t.getCause() != null ? t.getCause() : t));
                    }
                }
            });
            ModuleLog.line(TAG + "✅ Hooked LikeActionView." + configure.getName() + " (" + LikeAnimations.selectable(names).size() + " animations)");
            FeatureStatusTracker.setHooked("CustomLikeAnimation");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }
}
