package io.github.somilkhan.instaeclipse2.mods.piko;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

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
 * Stops a swipe up in a chat from turning disappearing (vanish) mode on (Piko's "Disable swipe
 * for disappearing messages").
 *
 * <p>458.0.0.0.57: the thread's swipe controller (X.07Lw, the class with "disappearing_messages")
 * wires the pull gesture in a ()V method (A01) that also sets the RefreshableNestedScrollingParent
 * refreshing distance. It asks two static checks: A05(UserSession, thread) "is vanish mode on"
 * and A07(UserSession, thread, true) "may this thread use the gesture"; when the second says no,
 * Instagram itself tears the gesture down (A03) and returns. We answer no while vanish mode is
 * off, so the swipe that turns it ON is gone, but a chat already in vanish mode keeps the swipe
 * to leave it, as in Piko. Re-applied whenever Instagram re-runs the setup (thread re-open).
 */
public class DisappearingSwipeHook {

    private static final String TAG = "(InstaEclipse | DisappearingSwipe): ";
    private static final ThreadLocal<int[]> IN_SETUP = ThreadLocal.withInitial(() -> new int[1]);

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            List<MethodData> setups = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create().usingStrings("disappearing_messages"))
                    .returnType("void").paramCount(0)
                    .addInvoke(MethodMatcher.create()
                            .declaredClass("com.instagram.ui.widget.refresh.RefreshableNestedScrollingParent")
                            .name("setRefreshingDistance"))));
            if (setups.isEmpty()) {
                // Seen on a 458.0.0.0.67 device build: no match with the "disappearing_messages"
                // class string. Fall back to the shape alone (the only ()V caller of
                // setRefreshingDistance); the vanish-check pair below must still match exactly.
                setups = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                        .returnType("void").paramCount(0)
                        .addInvoke(MethodMatcher.create()
                                .declaredClass("com.instagram.ui.widget.refresh.RefreshableNestedScrollingParent")
                                .name("setRefreshingDistance"))));
                if (setups.size() == 1) ModuleLog.line(TAG + "setup found without the class string: " + setups.get(0).getClassName() + "." + setups.get(0).getName());
            }
            if (setups.size() != 1) {
                ModuleLog.line(TAG + "❌ expected 1 swipe setup method, found " + setups.size());
                if (FeatureFlags.disableDisappearingSwipe) FeatureStatusTracker.setBroken("DisableDisappearingSwipe");
                return;
            }
            MethodData setupData = setups.get(0);
            Method isActive = null, gate = null;
            int na = 0, ng = 0;
            for (MethodData inv : setupData.getInvokes()) {
                if (!inv.isMethod()) continue;
                List<String> p = inv.getParamTypeNames();
                if (!"boolean".equals(inv.getReturnTypeName()) || p.isEmpty()
                        || !"com.instagram.common.session.UserSession".equals(p.get(0))) continue;
                Method m;
                try { m = inv.getMethodInstance(cl); } catch (Throwable t) { continue; }
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (p.size() == 2 && !(isActive != null && isActive.equals(m))) { isActive = m; na++; }
                else if (p.size() == 3 && "boolean".equals(p.get(2)) && !(gate != null && gate.equals(m))) { gate = m; ng++; }
            }
            if (na != 1 || ng != 1 || isActive.getDeclaringClass() != gate.getDeclaringClass()
                    || isActive.getParameterTypes()[1] != gate.getParameterTypes()[1]) {
                ModuleLog.line(TAG + "❌ vanish checks not found (active=" + na + ", gate=" + ng + ")");
                if (FeatureFlags.disableDisappearingSwipe) FeatureStatusTracker.setBroken("DisableDisappearingSwipe");
                return;
            }
            final Method active = isActive;
            active.setAccessible(true);
            Method setup = setupData.getMethodInstance(cl);

            XposedBridge.hookMethod(setup, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    IN_SETUP.get()[0]++;
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    IN_SETUP.get()[0]--;
                }
            });
            XposedBridge.hookMethod(gate, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.disableDisappearingSwipe || IN_SETUP.get()[0] <= 0) return;
                    if (!Boolean.TRUE.equals(param.getResult())) return;
                    try {
                        if (Boolean.TRUE.equals(active.invoke(null, param.args[0], param.args[1]))) return;
                    } catch (Throwable t) {
                        return; // unknown state: leave Instagram's answer
                    }
                    param.setResult(false);
                }
            });
            Deopt.caller(setup);
            ModuleLog.line(TAG + "✅ Hooked " + setup.getDeclaringClass().getName() + "." + setup.getName()
                    + " gate " + gate.getDeclaringClass().getName() + "." + gate.getName());
            FeatureStatusTracker.setHooked("DisableDisappearingSwipe");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
            if (FeatureFlags.disableDisappearingSwipe) FeatureStatusTracker.setBroken("DisableDisappearingSwipe");
        }
    }
}
