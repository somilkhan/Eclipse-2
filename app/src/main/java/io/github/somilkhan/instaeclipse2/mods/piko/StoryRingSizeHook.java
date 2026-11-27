package io.github.somilkhan.instaeclipse2.mods.piko;

import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Custom story tray avatar size (Piko's "Story ring size").
 *
 * <p>458.0.0.0.57: the tray's dimension holder (X.07iK, constructor
 * (Context, UserSession, Integer, float, int, int, int, int, boolean)) computes the avatar size as
 * the screen width split into ~3.75 or 5 slots, clamped between dp(66) and dp(100); dp() is a
 * static (Context, float)float converter (X.07td.A06). While that constructor runs on a thread we
 * swap both bounds for the chosen size, so the avatar is exactly that size and every derived
 * value (ring stroke, badge, spacing) follows from Instagram's own math.
 *
 * <p>The converter is used all over the app, so this hook is only installed when the option is on
 * at startup (changing it needs an Instagram restart), and it only rewrites 66/100 on the thread
 * that is inside the constructor.
 */
public class StoryRingSizeHook {

    private static final String TAG = "(InstaEclipse | StoryRingSize): ";
    private static volatile Thread ringThread;

    public void install(DexKitBridge bridge) {
        if (!FeatureFlags.customStoryRingSize) return;
        try {
            ClassLoader cl = Module.hostClassLoader;
            List<MethodData> ctors = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .name("<init>").paramCount(9).usingNumbers(66.0f, 100.0f)));
            MethodData ctorData = null;
            int n = 0;
            for (MethodData md : ctors) {
                List<String> p = md.getParamTypeNames();
                if (!"android.content.Context".equals(p.get(0)) || !"com.instagram.common.session.UserSession".equals(p.get(1))) continue;
                ctorData = md;
                n++;
            }
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 tray dimension constructor, found " + n);
                return;
            }
            Method dp = null;
            int nd = 0;
            for (MethodData inv : ctorData.getInvokes()) {
                if (!inv.isMethod() || inv.getDeclaredClassName().equals(ctorData.getDeclaredClassName())) continue;
                List<String> p = inv.getParamTypeNames();
                if (!"float".equals(inv.getReturnTypeName()) || p.size() != 2
                        || !"android.content.Context".equals(p.get(0)) || !"float".equals(p.get(1))) continue;
                Method m;
                try { m = inv.getMethodInstance(cl); } catch (Throwable t) { continue; }
                if (!Modifier.isStatic(m.getModifiers()) || (dp != null && dp.equals(m))) continue;
                dp = m;
                nd++;
            }
            if (nd != 1) {
                ModuleLog.line(TAG + "❌ expected 1 dp converter, found " + nd);
                return;
            }
            Member ctor = ctorData.getConstructorInstance(cl);
            XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    ringThread = Thread.currentThread();
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ringThread = null;
                }
            });
            XposedBridge.hookMethod(dp, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // First, always: this converter is hot, and its hook stub must never be JIT-compiled.
                    JitGuard.tick(param.method);
                    if (ringThread != Thread.currentThread()) return;
                    if (!(param.args[0] instanceof Context) || !(param.args[1] instanceof Float)) return;
                    float v = (Float) param.args[1];
                    float r = StoryRingSize.replace(v, FeatureFlags.storyRingSizeDp);
                    if (r != v) param.args[1] = r;
                }
            });
            Deopt.caller(ctor);
            ModuleLog.line(TAG + "✅ Hooked " + ctorData.getDeclaredClassName() + ".<init> via "
                    + dp.getDeclaringClass().getName() + "." + dp.getName() + " = " + StoryRingSize.clamp(FeatureFlags.storyRingSizeDp) + "dp");
            FeatureStatusTracker.setHooked("CustomStoryRingSize");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }
}
