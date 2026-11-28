package io.github.somilkhan.instaeclipse2.mods.piko;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.IgModels;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Filters the story tray by reel type and by verified / unverified owner (Piko's "Filter
 * stories"). Piko's min/max story count option isn't ported: the tray model exposes no reliable
 * item count on 458.
 *
 * <p>458.0.0.0.57: ReelStore's static synchronized merge (X.04nc.A0H, string
 * "ReelStore.maybeAddNewTrayReelResponseItemIntfs") receives the tray response items as a List of
 * {@code com.instagram.model.reels.ReelResponseItemIntf}. We replace that argument with a
 * filtered copy before the merge, the same point Piko patches. Reel type is the interface's
 * no-arg enum getter whose values include "ads_reel" (toString is the server value); the owner is
 * the getter returning the merge's user type. Both fall back to the generic Pando accessor.
 */
public class StoryTrayFilterHook {

    private static final String TAG = "(InstaEclipse | StoryFilter): ";
    private static final String INTF = "com.instagram.model.reels.ReelResponseItemIntf";

    private static Method reelTypeGetter, userGetter;

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            Class<?> intf = Class.forName(INTF, false, cl);
            Method target = null;
            int n = 0, listIndex = -1;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("void").usingStrings("ReelStore.maybeAddNewTrayReelResponseItemIntfs")))) {
                Method m = md.getMethodInstance(cl);
                if (!Modifier.isStatic(m.getModifiers())) continue;
                Class<?>[] p = m.getParameterTypes();
                int li = -1, lists = 0;
                for (int i = 0; i < p.length; i++) if (p[i] == List.class) { if (li < 0) li = i; lists++; }
                if (lists < 1 || p.length < 2) continue;
                target = m;
                listIndex = li;
                n++;
            }
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 tray merge method, found " + n);
                return;
            }
            Class<?> userType = target.getParameterTypes()[1];
            resolveGetters(intf, userType);
            final int idx = listIndex;
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.filterStoryTray) return;
                    Object arg = param.args[idx];
                    if (!(arg instanceof List)) return;
                    Set<String> types = StoryTrayFilter.parseCsv(FeatureFlags.storyFilterTypes);
                    Set<String> users = StoryTrayFilter.parseCsv(FeatureFlags.storyFilterUsers);
                    if (StoryTrayFilter.isNoop(types, users)) return;
                    List<?> items = (List<?>) arg;
                    List<Object> kept = new ArrayList<>(items.size());
                    int dropped = 0;
                    for (Object item : items) {
                        boolean hide = false;
                        try {
                            if (intf.isInstance(item)) hide = StoryTrayFilter.hide(reelType(item), verified(item), types, users);
                        } catch (Throwable ignored) {
                        }
                        if (hide) dropped++;
                        else kept.add(item);
                    }
                    if (dropped > 0) param.args[idx] = kept;
                }
            });
            ModuleLog.line(TAG + "✅ Hooked " + target.getDeclaringClass().getName() + "." + target.getName()
                    + " (type getter " + (reelTypeGetter != null ? reelTypeGetter.getName() : "pando")
                    + ", user getter " + (userGetter != null ? userGetter.getName() : "pando") + ")");
            FeatureStatusTracker.setHooked("FilterStoryTray");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    private static void resolveGetters(Class<?> intf, Class<?> userType) {
        Method type = null, user = null;
        int nt = 0, nu = 0;
        for (Method m : intf.getMethods()) {
            if (m.getParameterTypes().length != 0 || Modifier.isStatic(m.getModifiers())) continue;
            Class<?> r = m.getReturnType();
            if (r.isEnum() && hasConstant(r, "ads_reel")) { type = m; nt++; }
            else if (r == userType) { user = m; nu++; }
        }
        reelTypeGetter = nt == 1 ? type : null;
        userGetter = nu == 1 ? user : null;
    }

    /**
     * True when one of the enum's server values ({@code toString()}, e.g. "ads_reel") or names is
     * {@code value}. Instagram's toString() runs app code, so a throw there only rules out that
     * enum instead of aborting the whole hook install.
     */
    static boolean hasConstant(Class<?> e, String value) {
        Object[] cs;
        try {
            cs = e.getEnumConstants();
        } catch (Throwable t) {
            return false;
        }
        if (cs == null) return false;
        for (Object c : cs) {
            try {
                if (value.equals(String.valueOf(c))) return true;
            } catch (Throwable ignored) {
                // this constant's toString() failed; keep checking the others
            }
        }
        return false;
    }

    private static String reelType(Object item) {
        try {
            if (reelTypeGetter != null) {
                Object v = reelTypeGetter.invoke(item);
                if (v != null) return String.valueOf(v);
            }
        } catch (Throwable ignored) {
        }
        return IgModels.readString(item, "reel_type");
    }

    private static Boolean verified(Object item) {
        Object user = null;
        try {
            if (userGetter != null) user = userGetter.invoke(item);
        } catch (Throwable ignored) {
        }
        if (user == null) user = IgModels.read(item, "user");
        return user == null ? null : IgModels.readBoolean(user, "is_verified");
    }
}
