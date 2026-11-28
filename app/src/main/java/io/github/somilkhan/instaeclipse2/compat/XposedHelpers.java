package io.github.somilkhan.instaeclipse2.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Host-JVM re-implementation of the subset of XposedHelpers that is pure reflection. Semantics
 * follow rovo89/XposedBridge (Apache-2.0): exact lookups walk superclasses for fields/methods,
 * "BestMatch" lookups accept assignable/boxed parameter types.
 */
public final class XposedHelpers {
    private XposedHelpers() {}

    private static final Map<Object, Map<String, Object>> additionalFields = new WeakHashMap<>();

    public static final class ClassNotFoundError extends Error {
        private static final long serialVersionUID = 1L;
        public ClassNotFoundError(Throwable cause) { super(cause); }
        public ClassNotFoundError(String detailMessage, Throwable cause) { super(detailMessage, cause); }
    }

    public static final class InvocationTargetError extends Error {
        private static final long serialVersionUID = 1L;
        public InvocationTargetError(Throwable cause) { super(cause); }
    }

    // ── classes ──────────────────────────────────────────────────────────────

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (classLoader == null) classLoader = XposedBridge.BOOTCLASSLOADER;
        try {
            return classForName(className, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundError(e);
        }
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try { return findClass(className, classLoader); } catch (ClassNotFoundError e) { return null; }
    }

    private static Class<?> classForName(String name, ClassLoader cl) throws ClassNotFoundException {
        switch (name) {
            case "int": return int.class;
            case "long": return long.class;
            case "boolean": return boolean.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "short": return short.class;
            case "float": return float.class;
            case "double": return double.class;
            case "void": return void.class;
        }
        if (name.endsWith("[]")) {
            Class<?> c = classForName(name.substring(0, name.length() - 2), cl);
            return java.lang.reflect.Array.newInstance(c, 0).getClass();
        }
        return Class.forName(name, false, cl);
    }

    // ── fields ───────────────────────────────────────────────────────────────

    public static Field findField(Class<?> clazz, String fieldName) {
        Field f = findFieldIfExists(clazz, fieldName);
        if (f == null) throw new NoSuchFieldError(clazz.getName() + "#" + fieldName);
        return f;
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields())
                if (f.getType() == type) { f.setAccessible(true); return f; }
        throw new NoSuchFieldError("Field of type " + type.getName() + " in class " + clazz.getName());
    }

    // ── methods ──────────────────────────────────────────────────────────────


    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook))
            throw new IllegalArgumentException("no callback defined");
        XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        Method m = findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));
        return XposedBridge.hookMethod(m, callback);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        return findAndHookMethod(findClass(className, classLoader), methodName, parameterTypesAndCallback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook))
            throw new IllegalArgumentException("no callback defined");
        XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        Constructor<?> c = findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));
        return XposedBridge.hookMethod(c, callback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        return findAndHookConstructor(findClass(className, classLoader), parameterTypesAndCallback);
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        return findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypes));
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        Class<?> clazz = findClass(className, classLoader);
        return findMethodExact(clazz, methodName, getParameterClasses(classLoader, parameterTypes));
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(clazz.getName() + '#' + methodName + Arrays.toString(parameterTypes) + "#exact");
        }
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        try { return findMethodExact(clazz, methodName, parameterTypes); } catch (Throwable e) { return null; }
    }

    public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        try { return findMethodExact(className, classLoader, methodName, parameterTypes); } catch (Throwable e) { return null; }
    }

    public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        List<Method> result = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (returnType != null && returnType != m.getReturnType()) continue;
            if (!Arrays.equals(m.getParameterTypes(), parameterTypes)) continue;
            m.setAccessible(true);
            result.add(m);
        }
        return result.toArray(new Method[0]);
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try { return findMethodExact(clazz, methodName, parameterTypes); } catch (NoSuchMethodError ignored) {}
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (isAssignable(parameterTypes, m.getParameterTypes())) { m.setAccessible(true); return m; }
            }
        }
        throw new NoSuchMethodError(clazz.getName() + '#' + methodName + Arrays.toString(parameterTypes) + "#bestmatch");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        return findMethodBestMatch(clazz, methodName, getParameterTypes(args));
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Class<?>[] t = parameterTypes.clone();
        for (int i = 0; i < t.length; i++) if (t[i] == null && args[i] != null) t[i] = args[i].getClass();
        return findMethodBestMatch(clazz, methodName, t);
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        return findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypes));
    }

    public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
        return findConstructorExact(findClass(className, classLoader), getParameterClasses(classLoader, parameterTypes));
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(parameterTypes);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(clazz.getName() + Arrays.toString(parameterTypes) + "#exact");
        }
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
        try { return findConstructorExact(clazz, parameterTypes); } catch (Throwable e) { return null; }
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        try { return findConstructorExact(clazz, parameterTypes); } catch (NoSuchMethodError ignored) {}
        for (Constructor<?> c : clazz.getDeclaredConstructors())
            if (isAssignable(parameterTypes, c.getParameterTypes())) { c.setAccessible(true); return c; }
        throw new NoSuchMethodError(clazz.getName() + Arrays.toString(parameterTypes) + "#bestmatch");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        return findConstructorBestMatch(clazz, getParameterTypes(args));
    }

    private static boolean isAssignable(Class<?>[] from, Class<?>[] to) {
        if (from.length != to.length) return false;
        for (int i = 0; i < from.length; i++) {
            if (from[i] == null) { if (to[i].isPrimitive()) return false; continue; }
            if (!box(to[i]).isAssignableFrom(box(from[i]))) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        if (c == short.class) return Short.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        return Void.class;
    }

    public static Class<?>[] getParameterTypes(Object... args) {
        Class<?>[] clazzes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) clazzes[i] = args[i] != null ? args[i].getClass() : null;
        return clazzes;
    }

    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
        List<Class<?>> out = new ArrayList<>();
        for (Object type : parameterTypesAndCallback) {
            if (type instanceof XC_MethodHook) break;
            if (type == null) throw new ClassNotFoundError("parameter type must not be null", null);
            if (type instanceof Class) out.add((Class<?>) type);
            else if (type instanceof String) out.add(findClass((String) type, classLoader));
            else throw new ClassNotFoundError("parameter type must either be specified as Class or String", null);
        }
        return out.toArray(new Class<?>[0]);
    }

    public static Class<?>[] getClassesAsArray(Class<?>... clazzes) { return clazzes; }

    // ── field access ─────────────────────────────────────────────────────────

    private static Object getField(Object obj, String name) {
        try { return findField(obj.getClass(), name).get(obj); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
    }

    private static void setField(Object obj, String name, Object value) {
        try { findField(obj.getClass(), name).set(obj, value); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
    }

    private static Object getStatic(Class<?> clazz, String name) {
        try { return findField(clazz, name).get(null); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
    }

    private static void setStatic(Class<?> clazz, String name, Object value) {
        try { findField(clazz, name).set(null, value); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
    }

    public static Object getObjectField(Object obj, String fieldName) { return getField(obj, fieldName); }
    public static Object getSurroundingThis(Object obj) { return getField(obj, "this$0"); }
    public static boolean getBooleanField(Object obj, String fieldName) { return (Boolean) getField(obj, fieldName); }
    public static int getIntField(Object obj, String fieldName) { return (Integer) getField(obj, fieldName); }
    public static long getLongField(Object obj, String fieldName) { return (Long) getField(obj, fieldName); }
    public static float getFloatField(Object obj, String fieldName) { return (Float) getField(obj, fieldName); }
    public static double getDoubleField(Object obj, String fieldName) { return (Double) getField(obj, fieldName); }
    public static void setObjectField(Object obj, String fieldName, Object value) { setField(obj, fieldName, value); }
    public static void setBooleanField(Object obj, String fieldName, boolean value) { setField(obj, fieldName, value); }
    public static void setIntField(Object obj, String fieldName, int value) { setField(obj, fieldName, value); }
    public static void setLongField(Object obj, String fieldName, long value) { setField(obj, fieldName, value); }
    public static void setFloatField(Object obj, String fieldName, float value) { setField(obj, fieldName, value); }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) { return getStatic(clazz, fieldName); }
    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) { return (Boolean) getStatic(clazz, fieldName); }
    public static int getStaticIntField(Class<?> clazz, String fieldName) { return (Integer) getStatic(clazz, fieldName); }
    public static long getStaticLongField(Class<?> clazz, String fieldName) { return (Long) getStatic(clazz, fieldName); }
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) { setStatic(clazz, fieldName, value); }
    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) { setStatic(clazz, fieldName, value); }
    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) { setStatic(clazz, fieldName, value); }
    public static void setStaticLongField(Class<?> clazz, String fieldName, long value) { setStatic(clazz, fieldName, value); }

    // ── invocation ───────────────────────────────────────────────────────────

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try { return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
        catch (InvocationTargetException e) { throw new InvocationTargetError(e.getCause()); }
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        try { return findMethodBestMatch(obj.getClass(), methodName, parameterTypes, args).invoke(obj, args); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
        catch (InvocationTargetException e) { throw new InvocationTargetError(e.getCause()); }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try { return findMethodBestMatch(clazz, methodName, args).invoke(null, args); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
        catch (InvocationTargetException e) { throw new InvocationTargetError(e.getCause()); }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        try { return findMethodBestMatch(clazz, methodName, parameterTypes, args).invoke(null, args); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
        catch (InvocationTargetException e) { throw new InvocationTargetError(e.getCause()); }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try { return findConstructorBestMatch(clazz, args).newInstance(args); }
        catch (IllegalAccessException e) { throw new IllegalAccessError(e.getMessage()); }
        catch (InvocationTargetException e) { throw new InvocationTargetError(e.getCause()); }
        catch (InstantiationException e) { throw new InstantiationError(e.getMessage()); }
    }

    // ── additional fields ────────────────────────────────────────────────────

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        synchronized (additionalFields) {
            return additionalFields.computeIfAbsent(obj, k -> new HashMap<>()).put(key, value);
        }
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        synchronized (additionalFields) {
            Map<String, Object> m = additionalFields.get(obj);
            return m == null ? null : m.get(key);
        }
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        synchronized (additionalFields) {
            Map<String, Object> m = additionalFields.get(obj);
            return m == null ? null : m.remove(key);
        }
    }

    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) { return setAdditionalInstanceField(clazz, key, value); }
    public static Object getAdditionalStaticField(Class<?> clazz, String key) { return getAdditionalInstanceField(clazz, key); }
    public static Object removeAdditionalStaticField(Class<?> clazz, String key) { return removeAdditionalInstanceField(clazz, key); }

    public static int getFirstParameterIndexByType(Member method, Class<?> type) {
        Class<?>[] classes = method instanceof Method ? ((Method) method).getParameterTypes() : ((Constructor<?>) method).getParameterTypes();
        for (int i = 0; i < classes.length; i++) if (classes[i] == type) return i;
        throw new NoSuchFieldError("No parameter of type " + type + " found in " + method);
    }

    static boolean isStatic(Member m) { return Modifier.isStatic(m.getModifiers()); }
}
