package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import hookverify.Recorder;

/**
 * Host-JVM stand-in for XposedBridge: never patches anything, it only records every hook
 * request into {@link Recorder} so the harness can report what each feature would hook.
 */
public final class XposedBridge {
    public static final ClassLoader BOOTCLASSLOADER = XposedBridge.class.getClassLoader();
    public static int XPOSED_BRIDGE_VERSION = 82;

    private XposedBridge() {}

    public static int getXposedVersion() { return 82; }

    public static synchronized void log(String text) { Recorder.log("XposedBridge: " + text); }

    public static synchronized void log(Throwable t) { Recorder.log("XposedBridge: " + t); }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (!(hookMethod instanceof Method) && !(hookMethod instanceof Constructor<?>))
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod);
        if (hookMethod.getDeclaringClass().isInterface())
            throw new IllegalArgumentException("Cannot hook interfaces: " + hookMethod);
        if (Modifier.isAbstract(hookMethod.getModifiers()))
            throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod);
        if (callback == null) throw new IllegalArgumentException("callback was null");
        Recorder.hook(hookMethod);
        return callback.new Unhook(hookMethod);
    }

    @Deprecated
    public static void unhookMethod(Member hookMethod, XC_MethodHook callback) {}

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName)) unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member constructor : hookClass.getDeclaredConstructors()) unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException,
            java.lang.reflect.InvocationTargetException {
        if (method instanceof Method) {
            ((Method) method).setAccessible(true);
            return ((Method) method).invoke(thisObject, args);
        }
        throw new IllegalArgumentException("invokeOriginalMethod: unsupported in harness: " + method);
    }
}
