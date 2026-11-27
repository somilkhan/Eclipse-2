package io.github.somilkhan.instaeclipse2.mods.piko;

import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * <p>Two layers, the same two Piko patches:
 * <ol>
 *   <li><b>Metadata.</b> {@code com.instagram.api.schemas.XDTUserActivationMetadataImpl(animation,
 *       Boolean enabled, ...)} carries the like animation Instagram decides to play. Without it
 *       (or with {@code enabled = false}) Instagram keeps the plain heart, whatever the view was
 *       set up with. Before the constructor runs we put in the picked animation and
 *       {@code TRUE}. This was missing before and is why the switch did nothing.</li>
 *   <li><b>View.</b> {@code com.instagram.ui.mediaactions.LikeActionView}'s
 *       configure(UserSession, render) calls {@code setUpCustomLikesAnimation(render)} only with a
 *       server gate on. We run that custom branch ourselves with the matching render value.</li>
 * </ol>
 * Class and enum constant names here are not obfuscated, so no DexKit search is needed.
 */
public class LikeAnimationHook {

    private static final String TAG = "(InstaEclipse | LikeAnimation): ";
    private static final String METADATA_CLASS = "com.instagram.api.schemas.XDTUserActivationMetadataImpl";
    private static final Random RANDOM = new Random();

    /** One random pick per process, so the metadata and the view agree (Piko does the same). */
    private static volatile String sessionRandom;

    private final AtomicBoolean loggedMeta = new AtomicBoolean();
    private final AtomicBoolean loggedView = new AtomicBoolean();

    public void install() {
        int layers = 0;
        Class<?> metaEnum = installMetadata();
        if (metaEnum != null) layers++;
        if (installView(metaEnum)) layers++;
        if (layers > 0) FeatureStatusTracker.setHooked("CustomLikeAnimation");
    }

    /**
     * Name to use now (as saved, or this session's random pick), or null to leave Instagram alone.
     * Callers resolve it against their own enum with {@link LikeAnimations#metadataName}.
     */
    private static String currentPick(List<String> names) {
        if (!FeatureFlags.customLikeAnimation) return null;
        String pref = FeatureFlags.likeAnimation;
        if (pref == null || pref.isEmpty()) return null;
        if (!LikeAnimations.RANDOM.equals(pref)) return pref;
        String s = sessionRandom;
        if (s == null) {
            s = LikeAnimations.pick(LikeAnimations.RANDOM, names, RANDOM);
            sessionRandom = s;
        }
        return s;
    }

    // ---- Layer 1: activation metadata -------------------------------------------------------

    private Class<?> installMetadata() {
        try {
            Class<?> meta = Class.forName(METADATA_CLASS, false, Module.hostClassLoader);
            Class<?> metaEnum = null;
            int hooked = 0;
            for (Constructor<?> c : meta.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length < 2 || !p[0].isEnum() || p[1] != Boolean.class) continue;
                if (metaEnum != null && metaEnum != p[0]) continue;
                metaEnum = p[0];
                final Map<String, Object> byName = constants(metaEnum);
                final List<String> names = new ArrayList<>(byName.keySet());
                XposedBridge.hookMethod(c, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        JitGuard.tick(param.method);
                        String pick = currentPick(names);
                        Object value = pick != null ? byName.get(LikeAnimations.metadataName(pick, names)) : null;
                        if (value == null) return;
                        param.args[0] = value;
                        param.args[1] = Boolean.TRUE;
                        if (loggedMeta.compareAndSet(false, true)) {
                            ModuleLog.line(TAG + "metadata set to " + ((Enum<?>) value).name());
                        }
                    }
                });
                hooked++;
            }
            if (hooked == 0) {
                ModuleLog.line(TAG + "⚠️ " + METADATA_CLASS + "(enum, Boolean, ...) not found");
                return null;
            }
            ModuleLog.line(TAG + "✅ Hooked activation metadata (" + hooked + " ctor, "
                    + LikeAnimations.selectable(constants(metaEnum).keySet()).size() + " animations)");
            return metaEnum;
        } catch (Throwable t) {
            ModuleLog.line(TAG + "⚠️ metadata: " + t);
            return null;
        }
    }

    // ---- Layer 2: LikeActionView ------------------------------------------------------------

    private boolean installView(Class<?> metaEnum) {
        try {
            Class<?> view = Class.forName("com.instagram.ui.mediaactions.LikeActionView", false, Module.hostClassLoader);
            Method setUp = null;
            for (Method m : view.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ("setUpCustomLikesAnimation".equals(m.getName()) && p.length == 1 && p[0].isEnum()) setUp = m;
            }
            if (setUp == null) {
                ModuleLog.line(TAG + "❌ setUpCustomLikesAnimation(enum) not found");
                return false;
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
                return false;
            }
            final Map<String, Object> renderByName = constants(render);
            final List<String> renderNames = new ArrayList<>(renderByName.keySet());
            if (LikeAnimations.selectable(renderNames).isEmpty()) {
                ModuleLog.line(TAG + "❌ render enum has no like animations");
                return false;
            }
            // Instagram's own metadata -> render mapping (public static render m(metadata)), when present.
            Method mapper = null;
            if (metaEnum != null) {
                for (Method m : render.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == render
                            && p.length == 1 && p[0] == metaEnum) {
                        mapper = m;
                        break;
                    }
                }
                if (mapper != null) mapper.setAccessible(true);
            }
            final Map<String, Object> metaByName = metaEnum != null ? constants(metaEnum) : null;
            final List<String> metaNames = metaByName != null ? new ArrayList<>(metaByName.keySet()) : null;
            final Method toRender = mapper;
            final Method setUpCustom = setUp;
            setUpCustom.setAccessible(true);
            XposedBridge.hookMethod(configure, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    JitGuard.tick(param.method);
                    Object value = null;
                    // Prefer the same pick as the metadata, mapped the way Instagram maps it.
                    if (toRender != null && metaNames != null) {
                        String pick = currentPick(metaNames);
                        Object meta = pick != null ? metaByName.get(LikeAnimations.metadataName(pick, metaNames)) : null;
                        if (meta != null) {
                            try {
                                value = toRender.invoke(null, meta);
                            } catch (Throwable ignored) {
                                value = null;
                            }
                        }
                    }
                    if (value == null) {
                        String pick = currentPick(renderNames);
                        value = pick != null ? renderByName.get(LikeAnimations.metadataName(pick, renderNames)) : null;
                    }
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
                        if (loggedView.compareAndSet(false, true)) {
                            ModuleLog.line(TAG + "view set up with " + ((Enum<?>) value).name());
                        }
                    } catch (Throwable t) {
                        ModuleLog.line(TAG + "⚠️ " + (t.getCause() != null ? t.getCause() : t));
                    }
                }
            });
            ModuleLog.line(TAG + "✅ Hooked LikeActionView." + configure.getName() + " ("
                    + LikeAnimations.selectable(renderNames).size() + " animations, mapper "
                    + (mapper != null ? "found" : "missing") + ")");
            return true;
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
            return false;
        }
    }

    private static Map<String, Object> constants(Class<?> e) {
        Map<String, Object> out = new HashMap<>();
        Object[] all = e.getEnumConstants();
        if (all != null) for (Object c : all) out.put(((Enum<?>) c).name(), c);
        return out;
    }
}
