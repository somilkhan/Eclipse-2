package io.github.somilkhan.instaeclipse2.mods.piko;

import android.content.Context;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Hides the audio/video call buttons in the DM thread header (Piko's "Hide chat call buttons").
 *
 * <p>458.0.0.0.57: X.04ZL.A06(Context, state, X.04ZL) builds the header's right-side buttons as an
 * ArrayList of button models (X.09KK); each model has a final field of the button-kind enum
 * (X.09KJ: AUDIO_CALL, VIDEO_CALL, COMBINED_CALL_V*, ...). We filter the list after it is built,
 * so Instagram lays out the remaining buttons itself. The existing "hide Blend/Threads" header
 * option is a different hook and is unaffected.
 */
public class ChatCallButtonsHook {

    private static final String TAG = "(InstaEclipse | ChatCallButtons): ";
    private static Class<?> kindEnum;
    private static final Map<Class<?>, Field> KIND_FIELD = new ConcurrentHashMap<>();

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            List<ClassData> enums = bridge.findClass(FindClass.create().matcher(ClassMatcher.create()
                    .superClass("java.lang.Enum").usingStrings("ONGOING_AUDIO_CALL", "COMBINED_CALL_V2", "VIDEO_CALL")));
            if (enums.size() != 1) {
                ModuleLog.line(TAG + "❌ expected 1 header button kind enum, found " + enums.size());
                return;
            }
            kindEnum = enums.get(0).getInstance(cl);

            Method target = null;
            int n = 0;
            for (MethodData md : bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("java.util.ArrayList").paramCount(3)
                    .addUsingField(org.luckypray.dexkit.query.matchers.FieldMatcher.create().declaredClass(kindEnum.getName()))))) {
                Method m = md.getMethodInstance(cl);
                Class<?>[] p = m.getParameterTypes();
                if (!Modifier.isStatic(m.getModifiers()) || p[0] != Context.class || p[2] != m.getDeclaringClass()) continue;
                target = m;
                n++;
            }
            if (n != 1) {
                ModuleLog.line(TAG + "❌ expected 1 header button list builder, found " + n);
                return;
            }
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                    if (!FeatureFlags.hideChatCallButtons) return;
                    Object r = param.getResult();
                    if (!(r instanceof ArrayList)) return;
                    try {
                        Iterator<?> it = ((ArrayList<?>) r).iterator();
                        while (it.hasNext()) {
                            Object b = it.next();
                            if (b != null && CallButtons.isCall(kindOf(b))) it.remove();
                        }
                    } catch (Throwable t) {
                        ModuleLog.line(TAG + "⚠️ " + t);
                    }
                }
            });
            ModuleLog.line(TAG + "✅ Hooked " + target.getDeclaringClass().getName() + "." + target.getName());
            FeatureStatusTracker.setHooked("HideChatCallButtons");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    private static String kindOf(Object button) throws IllegalAccessException {
        Class<?> c = button.getClass();
        Field f = KIND_FIELD.get(c);
        if (f == null) {
            for (Field cand : c.getDeclaredFields()) {
                if (!Modifier.isStatic(cand.getModifiers()) && cand.getType() == kindEnum) {
                    if (f != null) return null; // two kind fields: unknown model, keep the button
                    f = cand;
                }
            }
            if (f == null) return null;
            f.setAccessible(true);
            KIND_FIELD.put(c, f);
        }
        Object k = f.get(button);
        return k instanceof Enum ? ((Enum<?>) k).name() : null;
    }
}
