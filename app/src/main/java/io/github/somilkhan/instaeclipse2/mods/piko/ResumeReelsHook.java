package io.github.somilkhan.instaeclipse2.mods.piko;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Resumes a reel where you left it when you scroll back to it (Piko's "Resume Reels").
 *
 * <p>458.0.0.0.57: the Reels player (X.0ArJ) pauses in a (String reason)int method (GfP) that
 * returns the current position; the viewer calls it with "out_of_playback_range" when a reel
 * scrolls out of view. The player keeps the reel it plays in a field (X.03yF, has getId()).
 * Preparing a reel goes through the method with "ClipsVideoPlayer.prepareVideo autoPlayEnabled="
 * whose 7th argument is the start position (it is logged as "pos="). We remember the position on
 * scroll-out and pass it as the start position the next time that reel is prepared from 0.
 *
 * <p>Limit: very short positions (under a second) aren't kept, and Instagram may still restart a
 * reel that it fully re-creates (e.g. after leaving the Reels tab).
 */
public class ResumeReelsHook {

    private static final String TAG = "(InstaEclipse | ResumeReels): ";
    private static final String SCROLLED_OUT = "out_of_playback_range";
    private static final ReelResumeStore STORE = new ReelResumeStore(64);

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            List<MethodData> prepares = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("boolean").usingStrings("ClipsVideoPlayer.prepareVideo autoPlayEnabled=")));
            if (prepares.size() != 1) {
                ModuleLog.line(TAG + "❌ expected 1 prepareVideo, found " + prepares.size());
                return;
            }
            Method prepare = prepares.get(0).getMethodInstance(cl);
            Class<?>[] pp = prepare.getParameterTypes();
            if (pp.length < 7 || pp[6] != int.class || pp[5] != float.class) {
                ModuleLog.line(TAG + "❌ prepareVideo shape changed");
                return;
            }
            Class<?> player = prepare.getDeclaringClass();
            Class<?> itemType = pp[1];
            Method getId = itemType.getMethod("getId");
            if (getId.getReturnType() != String.class) {
                ModuleLog.line(TAG + "❌ reel item has no String getId()");
                return;
            }
            Method pause = null;
            int np = 0;
            for (Method m : player.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() != int.class || p.length != 1 || p[0] != String.class) continue;
                pause = m;
                np++;
            }
            Field item = null;
            int nf = 0;
            for (Field f : player.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && f.getType() == itemType) { item = f; nf++; }
            }
            if (np != 1 || nf != 1) {
                ModuleLog.line(TAG + "❌ pause/item not unique (pause=" + np + ", item=" + nf + ")");
                return;
            }
            item.setAccessible(true);
            final Field itemField = item;

            XposedBridge.hookMethod(pause, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.resumeReels || !SCROLLED_OUT.equals(param.args[0])) return;
                    Object r = param.getResult();
                    if (!(r instanceof Integer)) return;
                    try {
                        Object reel = itemField.get(param.thisObject);
                        if (reel != null) STORE.save((String) getId.invoke(reel), (Integer) r);
                    } catch (Throwable ignored) {
                    }
                }
            });
            XposedBridge.hookMethod(prepare, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.resumeReels || !(param.args[6] instanceof Integer) || (Integer) param.args[6] != 0) return;
                    Object reel = param.args[1];
                    if (reel == null) return;
                    try {
                        int pos = STORE.take((String) getId.invoke(reel));
                        if (pos > 0) param.args[6] = pos;
                    } catch (Throwable ignored) {
                    }
                }
            });
            ModuleLog.line(TAG + "✅ Hooked " + player.getName() + "." + pause.getName() + " / " + prepare.getName());
            FeatureStatusTracker.setHooked("ResumeReels");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }
}
