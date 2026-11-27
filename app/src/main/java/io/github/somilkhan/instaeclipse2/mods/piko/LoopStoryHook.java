package io.github.somilkhan.instaeclipse2.mods.piko;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Replays the current story when it ends instead of moving on (Piko's "Loop story").
 *
 * <p>ReelViewerFragment's completion handler (A1U(ReelItem) on 458.0.0.0.57, the one using
 * "end_scene" and "comment") is called both when a photo's timer runs out and when a video
 * finishes. Before advancing it asks a private (ReelItem)boolean check (A1G) whether Instagram's
 * own server-gated "replay" experiment applies; when that says yes, Instagram resets the photo
 * timer and the progress bar itself and stays on the item. We answer yes for the duration of the
 * completion call, so photos loop through Instagram's own code path.
 *
 * <p>That path doesn't rewind videos, so right after it we seek the story video player
 * (field {@code mVideoPlayer}) back to 0 and resume it, but only when the player is still on the
 * same item. The player has a relative and an absolute (int) seek; the absolute one is the one
 * whose implementation doesn't read the current position.
 */
public class LoopStoryHook {

    private static final String TAG = "(InstaEclipse | LoopStory): ";
    private static final String FRAGMENT = "instagram.features.stories.fragment.ReelViewerFragment";

    private static final ThreadLocal<int[]> IN_COMPLETION = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<boolean[]> FORCED = ThreadLocal.withInitial(() -> new boolean[1]);

    private static Field playerField;
    private static String seekToName;
    private static Method resumeMethod; // interface (String, boolean)V

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            Class<?> fragment = Class.forName(FRAGMENT, false, cl);
            Class<?> reelItem = Class.forName("com.instagram.model.reels.ReelItem", false, cl);

            List<MethodData> found = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .declaredClass(FRAGMENT).returnType("void").paramTypes("com.instagram.model.reels.ReelItem")
                    .usingStrings("end_scene", "comment")));
            if (found.size() != 1) {
                ModuleLog.line(TAG + "❌ expected 1 story completion method, found " + found.size());
                return;
            }
            MethodData completionData = found.get(0);
            Method completion = completionData.getMethodInstance(cl);

            Method replayCheck = null;
            int n = 0;
            for (MethodData inv : completionData.getInvokes()) {
                if (!FRAGMENT.equals(inv.getDeclaredClassName()) || !inv.isMethod()) continue;
                Method m;
                try { m = inv.getMethodInstance(cl); } catch (Throwable t) { continue; }
                if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() != boolean.class) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1 || p[0] != reelItem || !Modifier.isPrivate(m.getModifiers())) continue;
                if (replayCheck != null && replayCheck.equals(m)) continue;
                replayCheck = m;
                n++;
            }
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 replay check in " + completion.getName() + ", found " + n);
                return;
            }

            resolveVideoPlayer(bridge, fragment);

            XposedBridge.hookMethod(completion, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    IN_COMPLETION.get()[0]++;
                    if (IN_COMPLETION.get()[0] == 1) FORCED.get()[0] = false;
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    int depth = --IN_COMPLETION.get()[0];
                    if (depth != 0 || !FORCED.get()[0]) return;
                    FORCED.get()[0] = false;
                    restartVideo(param.thisObject, param.args[0]);
                }
            });
            XposedBridge.hookMethod(replayCheck, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.loopStories || IN_COMPLETION.get()[0] <= 0) return;
                    FORCED.get()[0] = true;
                    param.setResult(true);
                }
            });
            Deopt.caller(completion); // keep A1G a real call (it's tiny and private)
            ModuleLog.line(TAG + "✅ Hooked " + completion.getName() + " / " + replayCheck.getName()
                    + (seekToName != null ? " (video seek " + seekToName + ")" : " (photos only)"));
            FeatureStatusTracker.setHooked("LoopStories");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    private static void resolveVideoPlayer(DexKitBridge bridge, Class<?> fragment) {
        try {
            Field f;
            try {
                f = fragment.getDeclaredField("mVideoPlayer");
            } catch (NoSuchFieldException e) {
                return;
            }
            Class<?> api = f.getType();
            if (!api.isInterface()) return;
            List<Method> seeks = new ArrayList<>();
            for (Method m : api.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() == void.class && p.length == 1 && p[0] == int.class) seeks.add(m);
                if (m.getReturnType() == void.class && p.length == 2 && p[0] == String.class && p[1] == boolean.class) {
                    if (resumeMethod != null) return; // ambiguous: don't guess
                    resumeMethod = m;
                }
            }
            if (seeks.size() != 2 || resumeMethod == null) { resumeMethod = null; return; }

            // Tell relative from absolute seek on one implementation: relative reads the position.
            List<ClassData> impls = bridge.findClass(FindClass.create().matcher(ClassMatcher.create().addInterface(api.getName())));
            String absolute = null;
            for (ClassData impl : impls) {
                String abs = null;
                int absCount = 0;
                for (Method s : seeks) {
                    List<MethodData> md = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                            .declaredClass(impl.getName()).name(s.getName()).paramTypes("int").returnType("void")));
                    if (md.size() != 1) { absCount = -1; break; }
                    boolean readsPosition = false;
                    for (MethodData inv : md.get(0).getInvokes()) {
                        if (inv.isMethod() && "int".equals(inv.getReturnTypeName()) && inv.getParamTypeNames().isEmpty()) {
                            readsPosition = true;
                            break;
                        }
                    }
                    if (!readsPosition) { abs = s.getName(); absCount++; }
                }
                if (absCount == 1) {
                    if (absolute != null && !absolute.equals(abs)) { absolute = null; break; }
                    absolute = abs;
                }
            }
            if (absolute == null) { resumeMethod = null; return; }
            f.setAccessible(true);
            playerField = f;
            seekToName = absolute;
        } catch (Throwable t) {
            resumeMethod = null;
            ModuleLog.line(TAG + "⚠️ video rewind unavailable: " + t);
        }
    }

    private static void restartVideo(Object fragment, Object item) {
        if (playerField == null || seekToName == null || resumeMethod == null || item == null) return;
        try {
            Object player = playerField.get(fragment);
            if (player == null || !sameItem(player, item)) return;
            Method seek = player.getClass().getMethod(seekToName, int.class);
            seek.invoke(player, 0);
            resumeMethod.invoke(player, "resume", false);
        } catch (Throwable t) {
            // Some players refuse to seek certain items (e.g. live/ad); the photo path still ran.
            ModuleLog.line(TAG + "⚠️ video rewind failed: " + (t.getCause() != null ? t.getCause() : t));
        }
    }

    /** The player holds its current item in a public ReelItem field. */
    private static boolean sameItem(Object player, Object item) {
        for (Class<?> c = player.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !f.getType().isInstance(item)) continue;
                if (f.getType() == Object.class) continue;
                try {
                    f.setAccessible(true);
                    if (f.get(player) == item) return true;
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }
}
