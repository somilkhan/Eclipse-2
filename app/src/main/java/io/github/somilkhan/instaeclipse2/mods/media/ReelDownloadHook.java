package io.github.somilkhan.instaeclipse2.mods.media;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.core.DexKitCache;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.i18n.I18n;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

public class ReelDownloadHook {

    private static Class<?> controllerClass;
    private static Method   hookMethod;

    private static Method buttonAdderMethod;
    private static Field  activityField;

    // Cached field path to the carousel position holder on the controller.
    // The position holder is identified structurally: a non-framework object field
    // whose class has exactly ONE int field (survives obfuscation renames).
    private static Field cachedOuterField = null;
    private static Field cachedInnerField = null;

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        installNativeDownloadGateUnlock(bridge, classLoader);
        installReduceOptionsListPatch(bridge, classLoader);

        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod("ReelDownload", classLoader);
            if (cached != null) {
                controllerClass = cached.getDeclaringClass();
                hookMethod = cached;
                cached.setAccessible(true);
                FeatureStatusTracker.setHooked("ReelDownload");
                XposedBridge.hookMethod(cached, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                        if (!FeatureFlags.enableReelDownload) return;
                        onOptionsBuilt(param);
                    }
                });
                ModuleLog.line("(IE|Reel) ✅ hooked: " + hookMethod.getDeclaringClass().getName() + "." + hookMethod.getName());
                return;
            }
        }

        try {
            var methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .usingStrings("ClipsOrganicMediaItemViewMoreOptionsController")));

            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Reel) ❌ ClipsOrganicMediaItemViewMoreOptionsController not found");
                return;
            }

            // The controller string can be referenced from more than one method; collect every
            // distinct declaring class and pick whichever actually carries the options-builder.
            java.util.LinkedHashSet<Class<?>> candidates = new java.util.LinkedHashSet<>();
            for (var md : methods) {
                try { candidates.add(md.getMethodInstance(classLoader).getDeclaringClass()); }
                catch (Throwable ignored) {}
            }
            ModuleLog.line("(IE|Reel) string matches=" + methods.size()
                    + " candidateClasses=" + candidates.size());
            installEligibilityGate457(bridge, classLoader, candidates);

            // Options-builder: void(com.instagram.feed.media.Media, <ButtonAdder>), where the
            // ButtonAdder param exposes an (Context, OnClickListener, String, int) add method.
            // Prefer that precise shape; fall back to the first void(Media, non-primitive).
            Method target = null;
            Method fallback = null;
            for (Class<?> cand : candidates) {
                int voidMedia = 0;
                for (Method m : cand.getDeclaredMethods()) {
                    if (m.getReturnType() != void.class) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length < 2) continue;
                    if (!params[0].getName().equals("com.instagram.feed.media.Media")) continue;
                    if (params[1].isPrimitive() || params[1] == String.class) continue;
                    voidMedia++;
                    if (fallback == null) fallback = m;
                    if (hasButtonAdderMethod(params[1])) { target = m; break; }
                }
                if (voidMedia > 0) {
                    ModuleLog.line("(IE|Reel) candidate " + cand.getName()
                            + " void(Media,X)=" + voidMedia);
                }
                if (target != null) { controllerClass = cand; break; }
            }
            if (target == null && fallback != null) {
                target = fallback;
                controllerClass = fallback.getDeclaringClass();
            }

            if (target == null) {
                // 457+: no (Media, ButtonAdder) builder any more — rows are added one by one.
                if (installSheetRowInjection(bridge, classLoader, candidates)) return;
                ModuleLog.line("(IE|Reel) ❌ hook method (Media, ButtonAdder)V not found");
                return;
            }

            target.setAccessible(true);
            hookMethod = target;
            DexKitCache.saveMethod("ReelDownload", target);
            FeatureStatusTracker.setHooked("ReelDownload");

            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                    if (!FeatureFlags.enableReelDownload) return;
                    onOptionsBuilt(param);
                }
            });
            ModuleLog.line("(IE|Reel) ✅ hooked: " + hookMethod.getDeclaringClass().getName() + "." + hookMethod.getName());

        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ install: " + t);
        }
    }

    /**
     * True when {@code adder} exposes the reel-menu button-adder method
     * {@code (Context, View.OnClickListener, String, int)} — the same shape
     * {@link #onOptionsBuilt} invokes. Identifies the correct options-builder overload
     * structurally, so it survives obfuscated renames across Instagram versions.
     */
    private static boolean hasButtonAdderMethod(Class<?> adder) {
        if (adder == null) return false;
        for (Method m : adder.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 4
                    && Context.class.isAssignableFrom(p[0])
                    && View.OnClickListener.class.isAssignableFrom(p[1])
                    && p[2] == String.class
                    && p[3] == int.class) return true;
        }
        return false;
    }

    // ── Reduced options-list patch ──────────────────────────────────────────────
    //
    // IG's newer, simplified reel overflow menu builds its option list via one
    // method that returns a plain ArrayList<MediaOption$Option> (SAVE/UNSAVE,
    // PLAYBACK_CONTROLS, WHY_AM_I_SEEING_THIS, INTERESTED, NOT_INTERESTED,
    // TAG_OPTIONS, REPORT, REQUEST_COMMUNITY_NOTE, DEBUG_STICKER_TRANSLATION) —
    // DOWNLOAD was dropped entirely from this list, unlike the older/fuller
    // overflow-menu code path. Found via field-usage matching on two of its
    // distinctive enum references. Appending DOWNLOAD to the returned (mutable)
    // ArrayList lets it flow through the same generic per-option row builder
    // (LX/5RY;->A0Q -> LX/QIy;->A04) used for every other option here — same
    // shared row primitive the post menu uses, so PostDownloadContextMenuHook's
    // app-wide click-handler hook already covers whatever dispatches its click.
    private static void installReduceOptionsListPatch(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            // Enum constants are resolved lazily on first call: the lookup below must not depend
            // on running Instagram code at install time.
            final Object[] opts = new Object[4]; // DOWNLOAD, COPY_LINK, info carrier, speed carrier
            final boolean[] resolved = {false};
            final Runnable resolve = () -> {
                if (resolved[0]) return;
                resolved[0] = true;
                opts[0] = mediaOption(classLoader, "DOWNLOAD");
                opts[1] = mediaOption(classLoader, "COPY_LINK");   // #117 — clicks handled by PostDownloadContextMenuHook
                opts[2] = mediaOption(classLoader, PostDownloadContextMenuHook.MEDIA_INFO_CARRIER); // #231
                opts[3] = mediaOption(classLoader, PostDownloadContextMenuHook.SPEED_CARRIER);      // #247
                if (opts[0] == null && opts[1] == null)
                    ModuleLog.line("(IE|Reel) ❌ DOWNLOAD/COPY_LINK enum values not found");
            };

            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                    resolve.run();
                    final Object download = opts[0], copyLink = opts[1], info = opts[2], speedRow = opts[3];
                    boolean wantDl = FeatureFlags.enableReelDownload && download != null;
                    boolean wantCl = FeatureFlags.copyMediaLink && copyLink != null;
                    boolean wantInfo = FeatureFlags.offerMediaInfo && info != null;
                    boolean wantSpeed = FeatureFlags.offerPlaybackSpeed && speedRow != null;
                    if (!wantDl && !wantCl && !wantInfo && !wantSpeed) return;
                    try {
                        Object result = param.getResult();
                        if (result instanceof List<?> list) {
                            @SuppressWarnings("unchecked")
                            List<Object> mutable = (List<Object>) list;
                            if (wantDl && !mutable.contains(download)) mutable.add(download);
                            if (wantCl && !mutable.contains(copyLink)) mutable.add(copyLink);
                            if (wantInfo && !mutable.contains(info)) mutable.add(info);
                            if (wantSpeed && !mutable.contains(speedRow)) mutable.add(speedRow);
                        }
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Reel) ❌ options-list patch failed: " + t);
                    }
                }
            };

            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod("ReelOptionsListBuilder", classLoader);
                if (cached != null) {
                    XposedBridge.hookMethod(cached, hook);
                    FeatureStatusTracker.setHooked("ReelDownload");
                    return;
                }
            }

            String optionDesc = "Lcom/instagram/feed/media/mediaoption/MediaOption$Option;";
            var methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .returnType("java.util.ArrayList")
                            .addUsingField(optionDesc + "->PLAYBACK_CONTROLS:" + optionDesc)
                            .addUsingField(optionDesc + "->UNSAVE:" + optionDesc)));

            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Reel) ⚠️ Reduced options-list builder not found");
                return;
            }

            Method target = methods.get(0).getMethodInstance(classLoader);
            target.setAccessible(true);
            XposedBridge.hookMethod(target, hook);
            DexKitCache.saveMethod("ReelOptionsListBuilder", target);
            FeatureStatusTracker.setHooked("ReelDownload");
            ModuleLog.line("(IE|Reel) ✅ Options-list patch hooked: " +
                    target.getDeclaringClass().getName() + "." + target.getName());

        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ installReduceOptionsListPatch: " + t);
        }
    }

    /** {@code MediaOption$Option.valueOf(name)}, or null when this build has no such constant. */
    static Object mediaOption(ClassLoader cl, String name) {
        try {
            Class<?> c = cl.loadClass(MEDIA_OPTION);
            for (Object v : c.getEnumConstants()) if (((Enum<?>) v).name().equals(name)) return v;
        } catch (Throwable ignored) { }
        return null;
    }

    static final String MEDIA_OPTION = "com.instagram.feed.media.mediaoption.MediaOption$Option";

    // ── 457+: per-row sheet builder ─────────────────────────────────────────────
    //
    // On 457 the reel overflow sheet (ClipsOrganicMediaItemViewMoreOptionsController) is built
    // by one big static method (X.0Ckr.A0B) that reads the Media from the clips item and adds
    // each row through a private add-row helper void(MediaOption$Option, RowHolder). DOWNLOAD
    // is only added behind server/MobileConfig eligibility. We remember the Media while the
    // builder runs and, right after the always-present SHARE row, add our own Download row via
    // the holder's (String, int, OnClickListener) adder; a later native DOWNLOAD row on the
    // same holder is skipped so it never shows twice.
    private static final ThreadLocal<Object[]> SHEET = new ThreadLocal<>(); // {media, controller}
    private static final java.util.Set<Object> ROWS_ADDED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private static boolean installSheetRowInjection(DexKitBridge bridge, ClassLoader cl,
                                                    java.util.Collection<Class<?>> candidates) {
        String opt = "L" + MEDIA_OPTION.replace('.', '/') + ";";
        try {
            for (Class<?> ctrl : candidates) {
                String ctrlName = ctrl.getName();
                var builders = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                        .declaredClass(ctrlName)
                        .returnType("void")
                        .addUsingField(opt + "->SHARE:" + opt)
                        .addUsingField(opt + "->DOWNLOAD:" + opt)));
                if (builders.isEmpty()) continue;
                var adders = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                        .declaredClass(ctrlName)
                        .returnType("void")
                        .paramTypes(MEDIA_OPTION, null)));
                java.util.List<Method> rowAdders = new java.util.ArrayList<>();
                java.util.Map<Method, Method> holderAdd = new java.util.HashMap<>();
                for (var md : adders) {
                    try {
                        Method m = md.getMethodInstance(cl);
                        if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                        if (!m.getParameterTypes()[0].getName().equals(MEDIA_OPTION)) continue;
                        Method add = null;
                        for (var inv : md.getInvokes()) {
                            if (!inv.isMethod() || !inv.getReturnTypeName().equals("void")) continue;
                            var pt = inv.getParamTypeNames();
                            if (pt.size() == 3 && pt.get(0).equals("java.lang.String") && pt.get(1).equals("int")
                                    && pt.get(2).equals("android.view.View$OnClickListener")) {
                                add = inv.getMethodInstance(cl);
                                break;
                            }
                        }
                        if (add == null) continue;
                        add.setAccessible(true);
                        rowAdders.add(m);
                        holderAdd.put(m, add);
                    } catch (Throwable ignored) { }
                }
                if (rowAdders.isEmpty()) continue;

                XC_MethodHook ctx = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                        Object media = null, controller = param.thisObject;
                        for (Object a : param.args) {
                            if (a == null) continue;
                            if (controller == null && ctrl.isInstance(a)) controller = a;
                            if (media == null) media = findMedia(a);
                        }
                        SHEET.set(new Object[]{media, controller});
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) { SHEET.remove(); }
                };
                int hooked = 0;
                for (var md : builders) {
                    try { XposedBridge.hookMethod(md.getMethodInstance(cl), ctx); hooked++; } catch (Throwable ignored) { }
                }
                for (Method m : rowAdders) {
                    m.setAccessible(true);
                    final Method add = holderAdd.get(m);
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                            if (!FeatureFlags.enableReelDownload || param.args[1] == null) return;
                            if ("DOWNLOAD".equals(((Enum<?>) param.args[0]).name())
                                    && ROWS_ADDED.contains(param.args[1])) param.setResult(null);
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!FeatureFlags.enableReelDownload) return;
                            Object[] sheet = SHEET.get();
                            Object holder = param.args[1];
                            if (sheet == null || sheet[0] == null || holder == null) return;
                            if (!"SHARE".equals(((Enum<?>) param.args[0]).name()) || ROWS_ADDED.contains(holder)) return;
                            try {
                                Activity act = findActivity(param.thisObject);
                                if (act == null) return;
                                final Object media = sheet[0], controller = sheet[1];
                                add.invoke(holder, I18n.t(act, R.string.ig_dl_title), resolveDownloadIcon(act),
                                        (View.OnClickListener) v -> startReelDownload(act, media, controller));
                                ROWS_ADDED.add(holder);
                            } catch (Throwable t) {
                                ModuleLog.line("(IE|Reel) ❌ add download row: " + t);
                            }
                        }
                    });
                }
                controllerClass = ctrl;
                FeatureStatusTracker.setHooked("ReelDownload");
                ModuleLog.line("(IE|Reel) ✅ sheet row injection: " + ctrlName + " builders=" + hooked
                        + " rowAdders=" + rowAdders.size());
                return true;
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ installSheetRowInjection: " + t);
        }
        return false;
    }

    /**
     * 457+: "can this reel be downloaded" is an instance boolean(MediaRef, UserSession) on the
     * clips download util (the class logging "ClipsDownloadUtil.saveVideoToLocalStorage"); the
     * reel sheet builders call it before adding the native DOWNLOAD row. Of the util's
     * boolean(X, UserSession) methods, the one the sheet controller invokes is the gate.
     * (The second, "restricted viewer" check is now an inline MobileConfig read and is not hooked.)
     */
    private static void installEligibilityGate457(DexKitBridge bridge, ClassLoader cl,
                                                  java.util.Collection<Class<?>> controllers) {
        try {
            Method gate = DexKitCache.isCacheValid() ? DexKitCache.loadMethod("ReelDownloadGate_457", cl) : null;
            if (gate == null) {
                java.util.Set<String> utilGates = new java.util.HashSet<>();
                for (var md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                        .usingStrings("ClipsDownloadUtil.saveVideoToLocalStorage")))) {
                    var cd = bridge.getClassData(md.getClassName());
                    if (cd == null) continue;
                    for (var m : cd.getMethods()) {
                        var pt = m.getParamTypeNames();
                        if (m.isMethod() && "boolean".equals(m.getReturnTypeName()) && pt.size() == 2
                                && pt.get(1).equals("com.instagram.common.session.UserSession")
                                && !java.lang.reflect.Modifier.isStatic(m.getModifiers()))
                            utilGates.add(m.getDescriptor());
                    }
                }
                outer:
                for (Class<?> ctrl : controllers) {
                    var cd = bridge.getClassData(ctrl.getName());
                    if (cd == null) continue;
                    for (var m : cd.getMethods()) {
                        for (var inv : m.getInvokes()) {
                            if (utilGates.contains(inv.getDescriptor())) { gate = inv.getMethodInstance(cl); break outer; }
                        }
                    }
                }
                if (gate == null) return;
                DexKitCache.saveMethod("ReelDownloadGate_457", gate);
            }
            gate.setAccessible(true);
            XposedBridge.hookMethod(gate, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                    if (FeatureFlags.enableReelDownload) param.setResult(true);
                }
            });
            FeatureStatusTracker.setHooked("ReelDownload");
            ModuleLog.line("(IE|Reel) ✅ 457 eligibility gate: " + gate.getDeclaringClass().getName() + "." + gate.getName());
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ 457 eligibility gate: " + t);
        }
    }

    /** The Media itself, or the first Media-typed field of a model object (e.g. the clips item). */
    static Object findMedia(Object o) {
        if (o == null) return null;
        if (o.getClass().getName().equals("com.instagram.feed.media.Media")) return o;
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().startsWith("android.") || c.getName().startsWith("java.")) break;
            for (Field f : c.getDeclaredFields()) {
                if (!f.getType().getName().equals("com.instagram.feed.media.Media")) continue;
                try { f.setAccessible(true); Object v = f.get(o); if (v != null) return v; } catch (Throwable ignored) { }
            }
        }
        return null;
    }

    private static Activity findActivity(Object controller) {
        if (controller == null) return null;
        for (Class<?> c = controller.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Activity.class.isAssignableFrom(f.getType())) continue;
                try { f.setAccessible(true); return (Activity) f.get(controller); } catch (Throwable ignored) { }
            }
        }
        return null;
    }

    // ── Native download-row unlock ──────────────────────────────────────────────
    //
    // IG 437+ moved the reel overflow menu to the same shared row-builder (QIy) used
    // by the post menu, and it already has a fully-working, native DOWNLOAD row —
    // gated behind two eligibility checks (a "can this media be downloaded" gate and
    // a "is the viewer restricted" gate). When both pass, native code adds the row
    // via the same QIy.A04 primitive posts use, with a working click handler already
    // wired to Instagram's own save-to-camera-roll flow. Bypassing the two gates is
    // far simpler and more robust than reconstructing that row/click machinery
    // ourselves. Found via each gate's distinct hardcoded MobileConfig param ID.
    private static void installNativeDownloadGateUnlock(DexKitBridge bridge, ClassLoader classLoader) {
        // "Can this media be downloaded" — force true.
        installGateHook(bridge, classLoader, "ReelDownloadGate_eligible",
                36313978552585585L, // 0x81035f00020d71
                "com.instagram.common.session.UserSession", "com.instagram.feed.media.Media",
                true);

        // "Is the viewer restricted from downloading" — force false.
        installGateHook(bridge, classLoader, "ReelDownloadGate_restricted",
                36313978552847731L, // 0x81035f00060d73
                "com.instagram.common.session.UserSession", "boolean",
                false);
    }

    private static void installGateHook(DexKitBridge bridge, ClassLoader classLoader,
                                         String cacheKey, long configId,
                                         String param1Type, String param2Type,
                                         boolean forcedResult) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                ModuleLog.line("(IE|Reel|DEBUG) gate fired: " + cacheKey + " enabled=" + FeatureFlags.enableReelDownload);
                if (FeatureFlags.enableReelDownload) param.setResult(forcedResult);
            }
        };

        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod(cacheKey, classLoader);
            if (cached != null) {
                XposedBridge.hookMethod(cached, hook);
                return;
            }
        }

        try {
            var methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramTypes(param1Type, param2Type)
                            .returnType("boolean")
                            .usingNumbers(configId)));

            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Reel) gate config " + configId + " absent (pre-457 gate; 457 uses the sheet row injection)");
                return;
            }

            Method target = methods.get(0).getMethodInstance(classLoader);
            target.setAccessible(true);
            XposedBridge.hookMethod(target, hook);
            DexKitCache.saveMethod(cacheKey, target);
            FeatureStatusTracker.setHooked("ReelDownload");
            ModuleLog.line("(IE|Reel) ✅ Gate unlocked: " +
                    target.getDeclaringClass().getName() + "." + target.getName() + " -> " + forcedResult);

        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ installGateHook(" + cacheKey + "): " + t);
        }
    }

    /**
     * Fallback index resolver: structurally locates the carousel position holder on the
     * controller. The holder is the unique non-framework field whose class has exactly
     * ONE int field — this property survives obfuscation renames across IG versions.
     * Values outside [0, 200) are excluded to filter out config constants.
     * Result is cached after first resolution.
     */
    private static int findReelCarouselIndex(Object controller) {
        if (controller == null) return 0;

        if (cachedOuterField != null && cachedInnerField != null) {
            try {
                Object holder = cachedOuterField.get(controller);
                if (holder != null) return cachedInnerField.getInt(holder);
            } catch (Throwable ignored) {}
            cachedOuterField = null;
            cachedInnerField = null;
        }

        int bestIdx = Integer.MAX_VALUE;
        Field bestOuter = null;
        Field bestInner = null;

        Class<?> c = controller.getClass();
        while (c != null && c != Object.class) {
            for (Field outerF : c.getDeclaredFields()) {
                if (outerF.getType().isPrimitive()) continue;
                String pkg = outerF.getType().getName();
                if (pkg.startsWith("android.") || pkg.startsWith("java.")
                        || pkg.startsWith("androidx.") || pkg.startsWith("kotlin.")) continue;
                outerF.setAccessible(true);
                Object nested;
                try { nested = outerF.get(controller); } catch (Throwable ignored) { continue; }
                if (nested == null) continue;

                Field singleIntField = null;
                int intCount = 0;
                Class<?> nc = nested.getClass();
                while (nc != null && nc != Object.class) {
                    String npkg = nc.getName();
                    if (npkg.startsWith("android.") || npkg.startsWith("java.")
                            || npkg.startsWith("androidx.") || npkg.startsWith("kotlin.")) break;
                    for (Field nf : nc.getDeclaredFields()) {
                        if (nf.getType() != int.class) continue;
                        intCount++;
                        singleIntField = nf;
                        if (intCount > 1) break;
                    }
                    if (intCount > 1) break;
                    nc = nc.getSuperclass();
                }

                if (intCount == 1 && singleIntField != null) {
                    singleIntField.setAccessible(true);
                    try {
                        int idx = singleIntField.getInt(nested);
                        if (idx >= 0 && idx < 200 && idx < bestIdx) {
                            bestIdx   = idx;
                            bestOuter = outerF;
                            bestInner = singleIntField;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            c = c.getSuperclass();
        }

        if (bestOuter != null) {
            cachedOuterField = bestOuter;
            cachedInnerField = bestInner;
            return bestIdx;
        }
        return 0;
    }

    /**
     * Primary index resolver: walks the activity's live view hierarchy for a
     * ViewPager / ViewPager2 / ReboundViewPager / horizontal RecyclerView whose adapter
     * item count equals {@code carouselSize} and returns its current data index.
     * Multiple unrelated carousels can coincidentally share the same item count (e.g.
     * two feed posts both showing 4 photos) — trusting the first DFS hit in that case
     * previously misattributed the index to the wrong post. So every match is collected
     * and the result is only trusted when exactly one candidate matches; otherwise the
     * caller falls back to the data-layer field.
     *
     * @return current position [0, carouselSize), or -1 if not found / ambiguous
     */
    static int findCarouselIndexFromView(Context ctx, int carouselSize) {
        if (!(ctx instanceof Activity)) return -1;
        try {
            View root = ((Activity) ctx).getWindow().getDecorView();
            List<Integer> matches = new java.util.ArrayList<>();
            collectCarouselMatches(root, carouselSize, matches);
            return matches.size() == 1 ? matches.get(0) : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** Returns adapter item count, trying RecyclerView-style then PagerAdapter-style. */
    private static int adapterCount(Object adapter) {
        try { return (int) adapter.getClass().getMethod("getItemCount").invoke(adapter); } catch (Throwable ignored) {}
        try { return (int) adapter.getClass().getMethod("getCount").invoke(adapter); } catch (Throwable ignored) {}
        return -1;
    }

    /**
     * Recursive DFS over the view tree, collecting the resolved index of every carousel
     * whose adapter size matches — does not stop at the first hit. ViewPager / ViewPager2 /
     * ReboundViewPager are AndroidX / Instagram common-UI classes — stable names, no obfuscation.
     */
    private static void collectCarouselMatches(View view, int carouselSize, List<Integer> out) {
        String cn = view.getClass().getName();

        // ViewPager / ViewPager2 / ReboundViewPager and any subclass
        if (cn.contains("ViewPager")) {
            try {
                Object adapter = view.getClass().getMethod("getAdapter").invoke(view);
                if (adapter != null && adapterCount(adapter) == carouselSize) {
                    // Standard pagers: getCurrentItem()
                    // ReboundViewPager (Instagram looping carousel): getCurrentDataIndex()
                    for (String getter : new String[]{
                            "getCurrentItem", "getCurrentDataIndex",
                            "getCurrentWrappedDataIndex", "getCurrentRawDataIndex"}) {
                        try {
                            int cur = (int) view.getClass().getMethod(getter).invoke(view);
                            if (cur >= 0) { out.add(cur); break; }
                        } catch (NoSuchMethodException ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Horizontal RecyclerView (carousel, not the vertical feed list)
        if (cn.contains("RecyclerView")) {
            try {
                Object adapter = view.getClass().getMethod("getAdapter").invoke(view);
                if (adapter != null && adapterCount(adapter) == carouselSize) {
                    Object lm = view.getClass().getMethod("getLayoutManager").invoke(view);
                    if (lm != null) {
                        try {
                            int orientation = (int) lm.getClass().getMethod("getOrientation").invoke(lm);
                            if (orientation != 0 /* HORIZONTAL */) lm = null;
                        } catch (Throwable ignored) {}
                        if (lm != null) {
                            Integer pos = null;
                            try {
                                int p = (int) lm.getClass()
                                        .getMethod("findFirstCompletelyVisibleItemPosition").invoke(lm);
                                if (p >= 0) pos = p;
                            } catch (Throwable ignored) {}
                            if (pos == null) {
                                try {
                                    int p = (int) lm.getClass()
                                            .getMethod("findFirstVisibleItemPosition").invoke(lm);
                                    if (p >= 0) pos = p;
                                } catch (Throwable ignored) {}
                            }
                            if (pos != null) out.add(pos);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectCarouselMatches(vg.getChildAt(i), carouselSize, out);
            }
        }
    }

    private static void onOptionsBuilt(XC_MethodHook.MethodHookParam param) {
        try {
            Object controller  = param.thisObject;
            Object media       = param.args[0];
            Object buttonAdder = param.args[1];

            if (activityField == null) {
                for (Field f : controller.getClass().getDeclaredFields()) {
                    if (Activity.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        activityField = f;
                        break;
                    }
                }
            }
            if (activityField == null) {
                ModuleLog.line("(IE|Reel) ❌ no Activity field on controller");
                return;
            }

            Activity activity = (Activity) activityField.get(controller);
            if (activity == null) return;

            if (buttonAdderMethod == null) {
                for (Method m : buttonAdder.getClass().getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length != 4) continue;
                    if (!Context.class.isAssignableFrom(p[0])) continue;
                    if (!View.OnClickListener.class.isAssignableFrom(p[1])) continue;
                    if (p[2] != String.class) continue;
                    if (p[3] != int.class) continue;
                    m.setAccessible(true);
                    buttonAdderMethod = m;
                    break;
                }
            }
            if (buttonAdderMethod == null) {
                ModuleLog.line("(IE|Reel) ❌ buttonAdderMethod not found");
                return;
            }

            int icon = resolveDownloadIcon(activity);
            final Activity actCopy      = activity;
            final Object mediaCopy      = media;
            final Object controllerCopy = controller;

            buttonAdderMethod.invoke(buttonAdder, activity,
                    (View.OnClickListener) v -> startReelDownload(actCopy, mediaCopy, controllerCopy),
                    I18n.t(activity, R.string.ig_dl_title), icon);

        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ onOptionsBuilt: " + t);
        }
    }

    private static void startReelDownload(Context ctx, Object media, Object controller) {
        SelectedMediaDownloads.captureAndShowPost(ctx, media, "reel");
    }

    private static void startReelVideoDownload(Context ctx, Object media, Object controller) {
        String username = FeedVideoDownloadHook.extractUsernameFromMediaObject(media);
        if (username == null) username = "reel";

        String mediaId = "0";
        try {
            Object id = media.getClass().getMethod("getId").invoke(media);
            if (id instanceof String s && !s.isEmpty()) mediaId = s;
        } catch (Throwable ignored) {}

        String videoUrl = FeedVideoDownloadHook.bestVideoUrlFromMedia(media);

        if (videoUrl != null) {
            final String fn        = FeedVideoDownloadHook.buildFilename(username, "reel", mediaId, true);
            final String finalUrl  = videoUrl;
            final String finalUser = username;
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_downloading_reel), Toast.LENGTH_SHORT).show();
            FeedVideoDownloadHook.executor.submit(() -> {
                try {
                    boolean delegated = FeedVideoDownloadHook.downloadAndSave(ctx, finalUrl, fn, true, finalUser);
                    if (!delegated) {
                        FeedVideoDownloadHook.mainHandler.post(() ->
                                io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_saved), Toast.LENGTH_SHORT).show());
                    }
                } catch (Throwable e) {
                    FeedVideoDownloadHook.mainHandler.post(() ->
                            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
                }
            });
            return;
        }

        // No direct video — may be a photo carousel reel
        List<String> allUrls = FeedVideoDownloadHook.extractAllUrlsFromMedia(ctx, media);
        if (allUrls.isEmpty()) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_url_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        // Primary: live view state (always correct). Fallback: data-layer field (stale at slide 0).
        int viewIndex    = findCarouselIndexFromView(ctx, allUrls.size());
        int currentIndex = viewIndex >= 0 ? viewIndex : findReelCarouselIndex(controller);

        final String finalUsername = username;
        final String finalMediaId  = mediaId;
        final int    finalIndex    = currentIndex;
        FeedVideoDownloadHook.mainHandler.post(() ->
                FeedVideoDownloadHook.showPostDownloadDialog(ctx, allUrls, finalUsername, finalMediaId, finalIndex));
    }

    /** Reads the icon drawable ID from MediaOption$Option.DOWNLOAD enum value. */
    private static int resolveDownloadIcon(Context ctx) {
        try {
            Class<?> optionClass = ctx.getClassLoader()
                    .loadClass("com.instagram.feed.media.mediaoption.MediaOption$Option");
            for (Object val : (Object[]) optionClass.getMethod("values").invoke(null)) {
                if (val.toString().contains("DOWNLOAD")) {
                    Field f = val.getClass().getField("iconDrawable");
                    return (int) f.get(val);
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}
