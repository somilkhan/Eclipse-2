package io.github.somilkhan.instaeclipse2.mods.feed;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Hides the share, save and repost buttons and the like count under feed posts.
 * Ported from Piko's "Hide share button", "Hide save buttons", "Hide reshare button" and the
 * like-count part of "Hide like buttons" (feed only).
 *
 * <p>Feed rows are drawn from one immutable state object. Its {@code toString} starts with
 * {@code "MediaUfiUiState(mediaForActionHandler="} (X.04d9 on 458.0.0.0.57: one constructor,
 * final fields, a static copy helper that goes through the constructor). The renderers skip a
 * control when its boolean is false, so the hook clears the chosen booleans right after the
 * constructor runs, before the object is published. That is the state Instagram itself builds
 * for a post that has, for example, sharing turned off.
 *
 * <p>Field names are obfuscated, so {@link BooleanStateFields} matches each boolean field to its
 * toString label on the first instance. If a label cannot be matched to exactly one field,
 * nothing is written for it.
 *
 * <p>Reels are not covered: on 458 the matching checks are inlined into larger methods
 * (for example {@code ClipsMediaItemInfoUtil_shouldShowShareButton}), not separate methods
 * that can be hooked.
 */
public class FeedUfiButtonsHook {

    private static final String TAG = "(InstaEclipse | FeedUfi): ";
    static final String MARKER = "MediaUfiUiState(mediaForActionHandler=";

    static final String SHARE = ", isShareEnabled=";
    static final String SHARE_COUNT = ", shouldShowShareCountInUfi=";
    static final String SAVE = ", isSaveEnabled=";
    static final String REPOST = ", isRepostButtonEnabled=";
    static final String LIKE_COUNT = ", shouldShowLikeCountInUfi=";
    static final List<String> LABELS = Arrays.asList(SHARE, SHARE_COUNT, SAVE, REPOST, LIKE_COUNT);

    /** Resolved on the first instance; then read-only. */
    private static volatile Map<String, Field> fields;
    private static volatile boolean resolveFailed;

    public void install(DexKitBridge bridge) {
        try {
            Class<?> state = null;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .name("toString").returnType("java.lang.String").paramCount(0)
                    .usingStrings(MARKER)))) {
                state = md.getMethodInstance(io.github.somilkhan.instaeclipse2.Xposed.Module.hostClassLoader)
                        .getDeclaringClass();
                break;
            }
            if (state == null) {
                ModuleLog.line(TAG + "❌ feed UFI state class not found");
                return;
            }
            Constructor<?>[] ctors = state.getDeclaredConstructors();
            if (ctors.length == 0) {
                ModuleLog.line(TAG + "❌ no constructor on " + state.getName());
                return;
            }
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!anyOn()) return;
                    apply(param.thisObject);
                }
            };
            for (Constructor<?> c : ctors) XposedBridge.hookMethod(c, hook);
            ModuleLog.line(TAG + "✅ Hooked " + ctors.length + " constructor(s) of " + state.getName());
            FeatureStatusTracker.setHooked("HideFeedShareButton");
            FeatureStatusTracker.setHooked("HideFeedSaveButton");
            FeatureStatusTracker.setHooked("HideFeedRepostButton");
            FeatureStatusTracker.setHooked("HideFeedLikeCount");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    private static boolean anyOn() {
        return FeatureFlags.hideFeedShareButton || FeatureFlags.hideFeedSaveButton
                || FeatureFlags.hideFeedRepostButton || FeatureFlags.hideFeedLikeCount;
    }

    private static void apply(Object state) {
        Map<String, Field> map = fieldsFor(state);
        if (map == null) return;
        if (FeatureFlags.hideFeedShareButton) {
            clear(map, state, SHARE);
            clear(map, state, SHARE_COUNT);
        }
        if (FeatureFlags.hideFeedSaveButton) clear(map, state, SAVE);
        if (FeatureFlags.hideFeedRepostButton) clear(map, state, REPOST);
        if (FeatureFlags.hideFeedLikeCount) clear(map, state, LIKE_COUNT);
    }

    private static Map<String, Field> fieldsFor(Object state) {
        Map<String, Field> map = fields;
        if (map != null || resolveFailed) return map;
        synchronized (FeedUfiButtonsHook.class) {
            if (fields != null || resolveFailed) return fields;
            try {
                // Safe to flip fields here: the object was just built on this thread and has not
                // been handed to anyone yet. resolve() restores every value it changes.
                Map<String, Field> resolved = BooleanStateFields.resolve(state, LABELS);
                for (String label : LABELS) {
                    Field f = resolved.get(label);
                    ModuleLog.line(TAG + (f != null ? "✅ " + label.trim() + " -> " + f.getName()
                            : "⚠️ " + label.trim() + " not resolved; left unchanged"));
                }
                fields = resolved;
            } catch (Throwable t) {
                resolveFailed = true;
                ModuleLog.line(TAG + "❌ resolve fields: " + t);
            }
            return fields;
        }
    }

    private static void clear(Map<String, Field> map, Object state, String label) {
        Field f = map.get(label);
        if (f == null) return;
        try {
            f.setBoolean(state, false);
        } catch (Throwable t) {
            ModuleLog.line(TAG + "⚠️ write " + label.trim() + ": " + t);
        }
    }
}
