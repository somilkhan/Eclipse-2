package io.github.somilkhan.instaeclipse2.mods.feed;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.utils.core.DexKitCache;
import io.github.somilkhan.instaeclipse2.utils.core.StringTables;
import io.github.somilkhan.instaeclipse2.utils.core.IgModels;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

public class HideSuggestedFeedItemsHook {

    private static final String CACHE_KEY_PARSER = "FeedItemParserClass";
    private static final String CACHE_KEY_HAS_LIKED = "MediaHasLikedGetter";

    // "has_liked" getter: Media.()Boolean on IG 447+, LiveTreeMediaDict.()Boolean on <= 446 (436)
    // — reached through the Media field of that type. Resolved once at install (#249).
    private static volatile Method hasLikedGetter;
    /** IG 458+: no typed getter; read "has_liked" through the model's generic accessor. */
    private static volatile Class<?> hashedMediaClass;

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        XC_MethodHook filterHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                if (!FeatureFlags.hideSuggestionsInFeed && !FeatureFlags.hideThreadsSuggestions
                        && !FeatureFlags.hideLikedPosts) return;

                Object result = param.getResult();
                if (result == null) return;

                // The parsed FeedItem is a large union type — exactly one of its many
                // optional fields is populated per server-sent unit. A real post always
                // has its Media field set; every suggestion/injection type (clips netego,
                // suggested users, Threads cross-promo units, ...) leaves it null.
                boolean hasMedia = false;
                boolean isThreadsUnit = false;
                Object media = null;
                for (Field f : result.getClass().getDeclaredFields()) {
                    String typeName = f.getType().getSimpleName();
                    if (typeName.equals("Media")) {
                        try {
                            f.setAccessible(true);
                            media = f.get(result);
                            if (media != null) { hasMedia = true; break; }
                        } catch (Throwable ignored) {}
                    } else if (typeName.contains("Threads") || typeName.startsWith("TextApp") || typeName.startsWith("XDTTextApp")) {
                        // Threads-in-feed cross-promo unit types (ThreadsInFeedUnit...,
                        // TextApp...UnitDictImpl, XDTTextApp...UnitDictImpl) — matched by
                        // simple name since these are stable, unobfuscated GraphQL-generated
                        // class names, unlike the surrounding obfuscated field names.
                        try {
                            f.setAccessible(true);
                            if (f.get(result) != null) isThreadsUnit = true;
                        } catch (Throwable ignored) {}
                    }
                }

                if (hasMedia) {
                    // Real post: only hidden when "Hide liked posts" is on and you already liked it.
                    // Applied at parse time, so a post liked now disappears on the next refresh.
                    if (FeatureFlags.hideLikedPosts && isLiked(media)) param.setResult(null);
                    return;
                }
                if (!FeatureFlags.hideSuggestionsInFeed && !FeatureFlags.hideThreadsSuggestions) return;

                boolean shouldHide = isThreadsUnit ? FeatureFlags.hideThreadsSuggestions
                                                    : FeatureFlags.hideSuggestionsInFeed;
                if (!shouldHide) return;

                param.setResult(null);
            }
        };

        resolveHasLikedGetter(bridge, classLoader);

        if (DexKitCache.isCacheValid()) {
            String cached = DexKitCache.loadString(CACHE_KEY_PARSER);
            if (cached != null) {
                try {
                    hookBridgeMethod(cached, classLoader, filterHook);
                    markHookedForEnabledFlags();
                    return;
                } catch (Throwable t) {
                    ModuleLog.line("(InstaEclipse | HideSuggested): ⚠️ Cache hook failed: " + t.getMessage());
                }
            }
        }

        try {
            // StringTables: on 458 these literals live in the shared X.0000 string table, and a
            // plain usingStrings() query would match the table itself (and hook nothing useful).
            List<MethodData> methods = StringTables.findMethodUsingStrings(bridge, null,
                    "clips_netego", "suggested_users", "Unknown FeedItem Type");
            // 458 dropped "Unknown FeedItem Type"; the FeedItem parser is the JSON parser that
            // reads the "clips_netego" key. Enum initialisers use that string too, so require
            // the parser method name.
            if (methods.isEmpty()) {
                methods = StringTables.findMethodUsingStrings(bridge,
                        m -> m.name("unsafeParseFromJson"), "clips_netego");
            }
            if (methods.isEmpty()) {
                methods = StringTables.findMethodUsingStrings(bridge, null, "clips_netego", "media_or_ad");
            }
            if (methods.isEmpty()) {
                methods = StringTables.findMethodUsingStrings(bridge, null,
                        "clips_netego", "stories_netego", "bloks_netego");
            }

            if (methods.isEmpty()) {
                ModuleLog.line("(InstaEclipse | HideSuggested): ❌ FeedItem parser not found.");
                return;
            }

            String targetClass = null;
            for (MethodData md : methods) {
                if (hasParserBridge(md.getClassName(), classLoader)) {
                    targetClass = md.getClassName();
                    break;
                }
            }
            if (targetClass == null) {
                ModuleLog.line("(InstaEclipse | HideSuggested): ❌ FeedItem parser not found ("
                        + methods.size() + " candidates without a parser bridge).");
                return;
            }
            DexKitCache.saveString(CACHE_KEY_PARSER, targetClass);
            hookBridgeMethod(targetClass, classLoader, filterHook);
            markHookedForEnabledFlags();
            ModuleLog.line("(InstaEclipse | HideSuggested): ✅ Hooked: " + targetClass);

        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | HideSuggested): ❌ Exception: " + t.getMessage());
        }
    }

    private static void resolveHasLikedGetter(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod(CACHE_KEY_HAS_LIKED, classLoader);
                if (cached != null) { hasLikedGetter = cached; return; }
            }
            for (String owner : new String[]{"com.instagram.feed.media.Media",
                    "com.instagram.feed.media.LiveTreeMediaDict"}) {
                List<MethodData> found;
                try {
                    found = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                            .declaredClass(owner)
                            .usingEqStrings(java.util.List.of("has_liked"))
                            .returnType("java.lang.Boolean")
                            .paramCount(0)));
                } catch (Throwable t) { continue; }
                if (found.size() != 1) continue;
                Method m = found.get(0).getMethodInstance(classLoader);
                m.setAccessible(true);
                hasLikedGetter = m;
                DexKitCache.saveMethod(CACHE_KEY_HAS_LIKED, m);
                ModuleLog.line("(InstaEclipse | HideLiked): ✅ has_liked getter " + owner + "." + m.getName());
                return;
            }
            try {
                Class<?> media = Class.forName(IgModels.MEDIA, false, classLoader);
                if (IgModels.accessor(media) != null) {
                    hashedMediaClass = media;
                    ModuleLog.line("(InstaEclipse | HideLiked): ✅ has_liked via " + IgModels.accessor(media).getName() + "(hash)");
                    return;
                }
            } catch (Throwable ignored) {
            }
            ModuleLog.line("(InstaEclipse | HideLiked): ❌ has_liked getter not found");
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | HideLiked): ❌ " + t.getMessage());
        }
    }

    public static boolean isLiked(Object media) {
        Method getter = hasLikedGetter;
        if (media == null) return false;
        if (getter == null) return isLikedHashed(media);
        try {
            Object target = media;
            Class<?> owner = getter.getDeclaringClass();
            if (!owner.isInstance(media)) {
                target = null;
                for (Field f : media.getClass().getDeclaredFields()) {
                    if (!owner.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    target = f.get(media);
                    break;
                }
                if (target == null) return false;
            }
            return Boolean.TRUE.equals(getter.invoke(target));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isLikedHashed(Object media) {
        Class<?> owner = hashedMediaClass;
        if (owner == null) return false;
        try {
            Object target = media;
            if (!owner.isInstance(media)) {
                target = null;
                for (Field f : media.getClass().getDeclaredFields()) {
                    if (!owner.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    target = f.get(media);
                    break;
                }
                if (target == null) return false;
            }
            return Boolean.TRUE.equals(IgModels.readBoolean(target, "has_liked"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** True when the class has the bridge parse methods this hook attaches to. */
    private static boolean hasParserBridge(String className, ClassLoader classLoader) {
        try {
            for (Method m : Class.forName(className, false, classLoader).getDeclaredMethods()) {
                if (m.isBridge() && m.getName().equals("unsafeParseFromJson")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void hookBridgeMethod(String className, ClassLoader classLoader, XC_MethodHook hook)
            throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className, false, classLoader);
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isBridge()) {
                XposedBridge.hookMethod(m, hook);
            }
        }
    }

    // Mark the feature(s) as hooked as soon as the filter is successfully attached, rather
    // than waiting for a matching feed item to actually be filtered at runtime. The status
    // toast is built ~1.5s after the main activity's onCreate — whether a suggestion/Threads
    // unit has scrolled into the feed by then is essentially random, so gating the status on
    // that produced a false ❌ even when the hook was installed and working correctly.
    private void markHookedForEnabledFlags() {
        if (FeatureFlags.hideSuggestionsInFeed) FeatureStatusTracker.setHooked("HideSuggestionsInFeed");
        if (FeatureFlags.hideThreadsSuggestions) FeatureStatusTracker.setHooked("HideThreadsSuggestions");
        if (FeatureFlags.hideLikedPosts && (hasLikedGetter != null || hashedMediaClass != null)) FeatureStatusTracker.setHooked("HideLikedPosts");
    }
}
