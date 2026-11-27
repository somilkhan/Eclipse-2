package io.github.somilkhan.instaeclipse2.mods.piko;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

/**
 * Small private/static callees (a boolean check, a gate) are prime candidates for ART inlining
 * in Instagram's AOT-compiled code; a hook on the callee is then never reached from that caller.
 * LSPosed (not the legacy api-82 stub we compile against) offers
 * {@code XposedBridge.deoptimizeMethod(Member)}, which drops the caller back to the interpreter so
 * it really calls the callee. Best effort: on other frameworks this is a no-op.
 */
final class Deopt {

    private static volatile Method deopt;
    private static volatile boolean missing;

    private Deopt() {}

    static boolean caller(Member m) {
        if (m == null || missing) return false;
        try {
            Method d = deopt;
            if (d == null) {
                d = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
                d.setAccessible(true);
                deopt = d;
            }
            d.invoke(null, m);
            return true;
        } catch (NoSuchMethodException e) {
            missing = true;
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
