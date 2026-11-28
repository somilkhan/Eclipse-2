package io.github.somilkhan.instaeclipse2.compat;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Chain;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/** API-102-backed bridge retaining the project's reviewed before/after callback semantics. */
public final class XposedBridge {
    public static final ClassLoader BOOTCLASSLOADER = XposedBridge.class.getClassLoader();
    private static volatile XposedInterface framework;
    private XposedBridge() {}
    public static void attach(XposedInterface api) { framework = api; }
    public static XposedInterface framework() {
        XposedInterface api = framework;
        if (api == null) throw new IllegalStateException("libxposed API is not attached");
        return api;
    }
    public static void log(String text) { android.util.Log.i("InstaEclipse", text); }
    public static void log(Throwable t) { android.util.Log.e("InstaEclipse", String.valueOf(t)); }

    public static XC_MethodHook.Unhook hookMethod(Member member, XC_MethodHook callback) {
        if (!(member instanceof Executable)) throw new IllegalArgumentException("Not executable: " + member);
        if (callback == null) throw new IllegalArgumentException("callback was null");
        XposedInterface.HookHandle handle = framework().hook((Executable) member)
                .setPriority(callback.priority)
                .intercept(chain -> invoke(callback, chain));
        return callback.new Unhook(member, handle);
    }
    private static Object invoke(XC_MethodHook callback, Chain chain) throws Throwable {
        XC_MethodHook.MethodHookParam p = new XC_MethodHook.MethodHookParam();
        p.method = chain.getExecutable(); p.thisObject = chain.getThisObject();
        java.util.List<Object> args = chain.getArgs(); p.args = args.toArray();
        callback.callBefore(p);
        if (!p.returnEarly) {
            try { p.setProceedingResult(chain.proceed(p.args)); }
            catch (Throwable t) { p.setProceedingThrowable(t); }
        }
        callback.callAfter(p);
        return p.getResultOrThrowable();
    }
    public static void unhookMethod(Member member, XC_MethodHook callback) { /* handle owns removal */ }
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> type, String name, XC_MethodHook cb) {
        Set<XC_MethodHook.Unhook> result = new HashSet<>();
        for (Method m : type.getDeclaredMethods()) if (m.getName().equals(name)) result.add(hookMethod(m, cb));
        return result;
    }
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> type, XC_MethodHook cb) {
        Set<XC_MethodHook.Unhook> result = new HashSet<>();
        for (Constructor<?> c : type.getDeclaredConstructors()) result.add(hookMethod(c, cb));
        return result;
    }
    public static Object invokeOriginalMethod(Member member, Object self, Object[] args) throws Throwable {
        if (member instanceof Method) return framework().getInvoker((Method) member).invokeSpecial(self, args);
        if (member instanceof Constructor) return framework().getInvoker((Constructor<?>) member).newInstanceSpecial((Class) member.getDeclaringClass(), args);
        throw new IllegalArgumentException("Unsupported executable: " + member);
    }
}
