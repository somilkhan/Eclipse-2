package io.github.somilkhan.instaeclipse2.mods.piko;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * "Mark as read after reply" (Piko port). With Ghost seen on, GhostDMSeenHook drops each read
 * receipt; with this on it also {@link #hold holds} the newest one per thread. When a message the
 * user sent in that thread is confirmed by the server, the held receipt is sent with Instagram's
 * own sender, so replying marks the chat read and just reading it doesn't.
 *
 * <p>Instagram 458: the receipt sender is the static (UserSession, X, threadId, messageId,
 * senderId)V method that builds "mark_thread_seen-"+id (X.0OCQ.A0A). Message sends are
 * subclasses of the mutation base whose single no-arg List method returns the DirectThreadKeys
 * (X.0GFs.Df1); the send-text subclass is found by "send_text_message". The success callback is
 * the (X, mutation)V method logging "Mutation Successfully Sent" (X.07Yv.Fpr).
 */
public final class MarkReadAfterReplyHook {

    private static final String TAG = "(InstaEclipse | MarkReadAfterReply): ";
    private static final PendingReceipts<Object[]> PENDING = new PendingReceipts<>();
    private static volatile Method sender;
    private static volatile Class<?> sendBase;
    private static volatile Method threadKeys;

    /** Called by GhostDMSeenHook when it blocks a receipt. */
    public static void hold(Method method, Object[] args) {
        if (!FeatureFlags.markReadAfterReply || sendBase == null || method == null || args == null) return;
        String thread = null;
        for (Object a : args) if (a instanceof String s) { thread = s; break; }
        if (thread == null) return;
        sender = method;
        PENDING.hold(thread, args.clone());
    }

    public void install(DexKitBridge bridge) {
        try {
            ClassLoader cl = Module.hostClassLoader;
            List<MethodData> text = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings("send_text_message").paramCount(0).returnType(String.class.getName())));
            Class<?> base = null;
            for (MethodData m : text) {
                Class<?> sup = m.getMethodInstance(cl).getDeclaringClass().getSuperclass();
                Method keys = sup == null ? null : listGetter(sup);
                if (keys != null) { base = sup; threadKeys = keys; break; }
            }
            if (base == null) { ModuleLog.line(TAG + "❌ message send class not found, skipping"); return; }

            List<MethodData> ok = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings("Mutation Successfully Sent").paramCount(2).returnType("void")));
            if (ok.size() != 1) { ModuleLog.line(TAG + "❌ " + ok.size() + " send callbacks, skipping"); return; }
            Method callback = ok.get(0).getMethodInstance(cl);
            final Class<?> sendClass = base;
            XposedBridge.hookMethod(callback, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.markReadAfterReply || PENDING.size() == 0) return;
                    Object mutation = param.args[1];
                    if (!sendClass.isInstance(mutation)) return;
                    try {
                        Object keys = threadKeys.invoke(mutation);
                        if (!(keys instanceof List<?> list)) return;
                        for (Object key : list)
                            for (String id : ThreadKeyIds.parse(String.valueOf(key))) send(PENDING.release(id));
                    } catch (Throwable t) {
                        ModuleLog.line(TAG + "⚠️ " + t);
                    }
                }
            });
            sendBase = base;
            ModuleLog.line(TAG + "✅ Hooked " + callback.getDeclaringClass().getName() + "." + callback.getName()
                    + " for " + base.getName());
            FeatureStatusTracker.setHooked("MarkReadAfterReply");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    private static void send(Object[] args) {
        Method m = sender;
        if (args == null || m == null) return;
        try {
            // invokeOriginalMethod skips GhostDMSeenHook, so this receipt really goes out.
            XposedBridge.invokeOriginalMethod(m, null, args);
        } catch (Throwable t) {
            ModuleLog.line(TAG + "⚠️ send failed: " + t);
        }
    }

    /** The only no-arg List method declared on the mutation base (the thread keys). */
    static Method listGetter(Class<?> c) {
        Method found = null;
        for (Method m : c.getDeclaredMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != List.class || Modifier.isStatic(m.getModifiers())) continue;
            if (found != null) return null;
            found = m;
        }
        if (found != null) found.setAccessible(true);
        return found;
    }
}
