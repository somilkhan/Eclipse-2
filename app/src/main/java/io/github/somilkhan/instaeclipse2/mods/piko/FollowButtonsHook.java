package io.github.somilkhan.instaeclipse2.mods.piko;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;
import io.github.somilkhan.instaeclipse2.utils.ui.ResIds;

/**
 * Hides the Follow button on stories and next to the author on Reels (Piko's "Hide follow
 * button on stories / Reels").
 *
 * <p>Stories (458.0.0.0.57): every story overlay piece is a "view component" whose abstract base
 * (X.0KbR) has a bind method (A0R, the one with "StoryLegacyViewComponent.") and a name getter
 * (A0T). The follow button component reports "StoryFollowButtonViewComponent"; it keeps its
 * button in view-stub holders (fields of an interface with setVisibility(int)/getView()) and binds
 * it in a static (View, state, component) method. After bind we hide the holders, and after the
 * static binder we keep the bound button view hidden, so flows that re-show it lose.
 *
 * <p>Reels: the button is a Litho-mounted {@code com.instagram.user.follow.FollowButton} with
 * id {@code inline_follow_button} (InlineFollowButtonComponent). We watch every FollowButtonBase
 * when it is attached and hide it only when it sits inside a Reels container, so the same
 * component in feed rows and profiles is untouched. Litho recycles views between hosts, so a
 * button that moves out of Reels is given back.
 */
public class FollowButtonsHook {

    private static final String TAG = "(InstaEclipse | FollowButtons): ";
    private static final String STORY_COMPONENT = "StoryFollowButtonViewComponent";
    private static final String[] REELS_ANCESTORS = {
            "clips_viewer_container", "clips_items_view", "clips_item_overlay_component",
            "clips_ufi_component", "clips_video_container"};

    private static Method nameGetter;
    private static final Map<Class<?>, Boolean> IS_FOLLOW_COMPONENT = new ConcurrentHashMap<>();
    private static final Set<Class<?>> BINDERS_HOOKED = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------- stories

    public void installStories(DexKitBridge bridge) {
        try {
            Method bind = null, name = null;
            int nb = 0, nn = 0;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("void").paramCount(1).usingStrings("StoryLegacyViewComponent.")))) {
                Method m = md.getMethodInstance(Module.hostClassLoader);
                if (Modifier.isStatic(m.getModifiers()) || m.getParameterTypes()[0] != Object.class) continue;
                bind = m;
                nb++;
            }
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("java.lang.String").paramCount(0).usingStrings(STORY_COMPONENT)))) {
                Method m = md.getMethodInstance(Module.hostClassLoader);
                if (Modifier.isStatic(m.getModifiers())) continue;
                name = m;
                nn++;
            }
            Method baseName = nb == 1 && nn == 1 ? baseNameGetter(bind.getDeclaringClass(), name) : null;
            if (baseName == null) {
                ModuleLog.line(TAG + "❌ story component base not found (bind=" + nb + ", name=" + nn
                        + (bind != null && name != null ? ", bind in " + bind.getDeclaringClass().getName()
                        + ", name in " + name.getDeclaringClass().getName() : "") + ")");
                return;
            }
            baseName.setAccessible(true);
            nameGetter = baseName;
            XposedBridge.hookMethod(bind, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.hideStoryFollowButton) return;
                    Object comp = param.thisObject;
                    if (comp == null || !isFollowComponent(comp)) return;
                    hideHolders(comp);
                    hookBinders(comp.getClass());
                }
            });
            ModuleLog.line(TAG + "✅ Hooked story component bind " + bind.getDeclaringClass().getName() + "." + bind.getName());
            FeatureStatusTracker.setHooked("HideStoryFollowButton");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ stories: " + t);
        }
    }

    /**
     * The component base's virtual name getter. On 457+ the base (holding the final bind method)
     * declares {@code String name()} and only the follow component overrides it to return
     * {@link #STORY_COMPONENT}, so the string lookup lands on the subclass. Returns the base
     * declaration (so {@code invoke} dispatches per component), the method itself when it is
     * declared on the base, or null when the two lookups are unrelated classes.
     */
    static Method baseNameGetter(Class<?> base, Method name) {
        if (base == null || name == null || !base.isAssignableFrom(name.getDeclaringClass())) return null;
        if (name.getDeclaringClass() == base) return name;
        try {
            Method m = base.getDeclaredMethod(name.getName());
            if (m.getReturnType() != String.class || Modifier.isStatic(m.getModifiers())) return null;
            return m;
        } catch (NoSuchMethodException notOverride) {
            return null;
        }
    }

    /** Name check needs an instance; cached per class after the first answer. */
    private static boolean isFollowComponent(Object comp) {
        Class<?> c = comp.getClass();
        Boolean cached = IS_FOLLOW_COMPONENT.get(c);
        if (cached != null) return cached;
        boolean yes;
        try {
            yes = STORY_COMPONENT.equals(nameGetter.invoke(comp));
        } catch (Throwable t) {
            yes = false;
        }
        IS_FOLLOW_COMPONENT.put(c, yes);
        return yes;
    }

    private static void hideHolders(Object comp) {
        for (Class<?> c = comp.getClass(); c != null && c != nameGetter.getDeclaringClass(); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !f.getType().isInterface()) continue;
                Method setVis = method(f.getType(), "setVisibility", int.class);
                if (setVis == null || method(f.getType(), "getView") == null) continue;
                try {
                    f.setAccessible(true);
                    Object holder = f.get(comp);
                    if (holder != null) setVis.invoke(holder, View.GONE);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** The component's static (View, ..., component) binders put the real button on screen. */
    private static void hookBinders(Class<?> comp) {
        if (!BINDERS_HOOKED.add(comp)) return;
        int n = 0;
        for (Method m : comp.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length < 2 || !View.class.isAssignableFrom(p[0]) || p[p.length - 1] != comp) continue;
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.hideStoryFollowButton) return;
                    ViewHider.keepHidden((View) param.args[0], () -> FeatureFlags.hideStoryFollowButton);
                }
            });
            n++;
        }
        ModuleLog.line(TAG + (n > 0 ? "✅" : "⚠️") + " story follow binders hooked: " + n + " in " + comp.getName());
    }

    // ------------------------------------------------------------------ reels

    public void installReels() {
        try {
            Class<?> base = Class.forName("com.instagram.user.follow.FollowButtonBase", false, Module.hostClassLoader);
            XC_MethodHook onCreate = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    View v = (View) param.thisObject;
                    v.addOnAttachStateChangeListener(REELS_WATCHER);
                }
            };
            int n = 0;
            for (Constructor<?> c : base.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 0 || p[0] != Context.class) continue;
                XposedBridge.hookMethod(c, onCreate);
                n++;
            }
            if (n == 0) {
                ModuleLog.line(TAG + "❌ no FollowButtonBase(Context, ...) constructor");
                return;
            }
            ModuleLog.line(TAG + "✅ Hooked " + n + " FollowButtonBase constructors");
            FeatureStatusTracker.setHooked("HideReelsFollowButton");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ reels: " + t);
        }
    }

    private static final View.OnAttachStateChangeListener REELS_WATCHER = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View v) {
            try {
                boolean inReels = FeatureFlags.hideReelsFollowButton && insideReels(v);
                // Placement is decided per attach: Litho has to detach a view to move it to another host.
                if (inReels) ViewHider.keepHidden(v, () -> FeatureFlags.hideReelsFollowButton);
                else if (ViewHider.isGuarded(v)) ViewHider.release(v); // recycled into a non-Reels host
            } catch (Throwable ignored) {
            }
        }

        @Override public void onViewDetachedFromWindow(View v) {}
    };

    private static boolean insideReels(View v) {
        Context ctx = v.getContext();
        int[] ids = new int[REELS_ANCESTORS.length];
        for (int i = 0; i < ids.length; i++) ids[i] = ResIds.id(ctx, REELS_ANCESTORS[i]);
        int depth = 0;
        for (ViewParent p = v.getParent(); p instanceof View && depth < 40; p = p.getParent(), depth++) {
            int id = ((View) p).getId();
            if (id == View.NO_ID || id == 0) continue;
            for (int want : ids) if (want != 0 && id == want) return true;
        }
        return false;
    }

    private static Method method(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params);
        } catch (Throwable t) {
            return null;
        }
    }
}
