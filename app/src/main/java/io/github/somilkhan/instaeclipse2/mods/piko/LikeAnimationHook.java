package io.github.somilkhan.instaeclipse2.mods.piko;

import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Plays a chosen custom like animation, or one random pick per Instagram session, instead of the
 * plain heart (Piko's "Custom like animation").
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
    private static final Object NO_RENDER = new Object();

    /** One random pick per process, so the metadata and the view agree (Piko does the same). */
    private static volatile String sessionRandom;

    private static final String EXTRA_ORIGINAL = "ie_like_original";

    /** Metadata enum resolver; set once the constructor is found. */
    private static volatile LikeAnimations.Resolver metaResolver;

    private final AtomicBoolean loggedMeta = new AtomicBoolean();
    private final AtomicBoolean loggedUntracked = new AtomicBoolean();
    private final AtomicBoolean loggedMapper = new AtomicBoolean();
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
     * Callers resolve it against their own enum with a {@link LikeAnimations.Resolver}.
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
                if (metaEnum == null) {
                    metaEnum = p[0];
                    final LikeAnimations.Resolver r = new LikeAnimations.Resolver(constants(metaEnum));
                    metaResolver = r;
                    // Already-built models follow later switch / pick changes (see LikeMetadataTracker).
                    LikeMetadataTracker.setPicker(() -> r.get(currentPick(r.names())));
                }
                final LikeAnimations.Resolver resolver = metaResolver;
                XposedBridge.hookMethod(c, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        JitGuard.tick(param.method);
                        Object value = resolver.get(currentPick(resolver.names()));
                        if (value == null) return;
                        param.setObjectExtra(EXTRA_ORIGINAL, new Object[] {param.args[0], param.args[1], value});
                        param.args[0] = value;
                        param.args[1] = LikeMetadataTracker.MARK;
                        if (loggedMeta.compareAndSet(false, true)) {
                            ModuleLog.line(TAG + "metadata set to " + ((Enum<?>) value).name());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object[] orig = (Object[]) param.getObjectExtra(EXTRA_ORIGINAL);
                        if (orig == null || param.thisObject == null) return;
                        boolean tracked = LikeMetadataTracker.track(param.thisObject, orig[2], orig[0], orig[1]);
                        if (!tracked && loggedUntracked.compareAndSet(false, true)) {
                            ModuleLog.line(TAG + "⚠️ " + param.thisObject.getClass().getName()
                                    + ": fields not identified; models built while on keep the custom"
                                    + " animation after it is turned off");
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
                    + LikeAnimations.selectable(metaResolver.names()).size() + " animations)");
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
            final LikeAnimations.Resolver renderResolver = new LikeAnimations.Resolver(constants(render));
            final List<String> renderNames = renderResolver.names();
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
            final LikeAnimations.Resolver metaRes = metaEnum != null ? metaResolver : null;
            // metadata constant -> mapped render constant (or NO_RENDER), so the mapper runs once each.
            final Map<Object, Object> mapped = new ConcurrentHashMap<>();
            final Method toRender = mapper;
            final Method setUpCustom = setUp;
            setUpCustom.setAccessible(true);
            XposedBridge.hookMethod(configure, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    JitGuard.tick(param.method);
                    Object value = null;
                    // Prefer the same pick as the metadata, mapped the way Instagram maps it.
                    if (toRender != null && metaRes != null) {
                        Object meta = metaRes.get(currentPick(metaRes.names()));
                        if (meta != null) {
                            Object r = mapped.get(meta);
                            if (r == null) {
                                r = NO_RENDER;
                                try {
                                    Object m = toRender.invoke(null, meta);
                                    // The mapper may fall back to a plain/unrecognized value.
                                    if (m instanceof Enum && LikeAnimations.isSelectable(((Enum<?>) m).name())) r = m;
                                } catch (Throwable ignored) {
                                }
                                if (r == NO_RENDER && loggedMapper.compareAndSet(false, true)) {
                                    ModuleLog.line(TAG + "mapper gave no animation for "
                                            + ((Enum<?>) meta).name() + "; using the render enum by name");
                                }
                                mapped.put(meta, r);
                            }
                            if (r != NO_RENDER) value = r;
                        }
                    }
                    if (value == null) value = renderResolver.get(currentPick(renderNames));
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
