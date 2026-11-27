package io.github.somilkhan.instaeclipse2.utils.core;

import androidx.annotation.Nullable;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Version-tolerant access to Instagram's generated model classes (Media, User, ...).
 *
 * <p>Two things changed in Instagram 458:
 * <ol>
 *   <li>{@code com.instagram.user.model.User} was renamed by the obfuscator (it is {@code X.02gr}
 *       in 458.0.0.0.67). It still reports {@code getTypeName() == "XDTUserDict"}, so we find it by
 *       that string and keep {@link #userClassName()} pointing at the right class.</li>
 *   <li>The typed per-field getters ({@code Media.A3t() -> has_liked}, ...) were deleted. Every
 *       model still has one generic accessor {@code Object x(int)} that switches on the JSON field
 *       name's {@link String#hashCode()} ({@code "has_liked".hashCode() == 63659464}). It exists in
 *       457 too ({@code C5s}), so {@link #read(Object, String)} works on both.</li>
 * </ol>
 *
 * <p>Hooks keep their existing typed-getter lookups first and fall back to this class, so older
 * Instagram builds behave exactly as before.
 */
public final class IgModels {

    private IgModels() {}

    public static final String LEGACY_USER = "com.instagram.user.model.User";
    public static final String MEDIA = "com.instagram.feed.media.Media";
    public static final String USER_TYPE_NAME = "XDTUserDict";

    private static final String CACHE_KEY_USER = "IgModels.userClass";

    private static volatile String userClass;
    private static volatile boolean resolved;
    private static final Map<Class<?>, Object> ACCESSORS = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    /** Resolved User model class name; the legacy name until {@link #ensure} found something else. */
    public static String userClassName() {
        String u = userClass;
        return u != null ? u : LEGACY_USER;
    }

    /** True when {@code o} is an instance of the User model (legacy or renamed). */
    public static boolean isUser(@Nullable Object o) {
        return o != null && o.getClass().getName().equals(userClassName());
    }

    /** True when {@code className} names the User model (legacy or renamed). */
    public static boolean isUserClass(@Nullable String className) {
        return className != null && (className.equals(userClassName()) || className.equals(LEGACY_USER));
    }

    /** Loads the User model class, or null. */
    @Nullable
    public static Class<?> userClass(ClassLoader cl) {
        try {
            return Class.forName(userClassName(), false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resolves the renamed classes once. Cheap after the first call; safe from any hook's install.
     */
    public static void ensure(@Nullable DexKitBridge bridge, ClassLoader cl) {
        if (resolved) return;
        synchronized (IgModels.class) {
            if (resolved) return;
            try {
                Class.forName(LEGACY_USER, false, cl);
                userClass = LEGACY_USER;
                resolved = true;
                return;
            } catch (Throwable ignored) {
                // Renamed (458+). Look it up below.
            }
            if (DexKitCache.isCacheValid()) {
                String cached = DexKitCache.loadString(CACHE_KEY_USER);
                if (cached != null && loads(cached, cl)) {
                    userClass = cached;
                    resolved = true;
                    return;
                }
            }
            if (bridge == null) return;
            String found = findByTypeName(bridge, cl, USER_TYPE_NAME);
            if (found != null) {
                userClass = found;
                DexKitCache.saveString(CACHE_KEY_USER, found);
                ModuleLog.line("(IE|Models) ✅ User model = " + found);
            } else {
                ModuleLog.line("(IE|Models) ❌ User model not found (XDTUserDict)");
            }
            resolved = true;
        }
    }

    /**
     * The concrete model class whose {@code getTypeName()} returns {@code typeName} and which has the
     * generic {@code Object x(int)} field accessor.
     */
    @Nullable
    static String findByTypeName(DexKitBridge bridge, ClassLoader cl, String typeName) {
        try {
            List<MethodData> hits = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .name("getTypeName")
                    .paramCount(0)
                    .returnType("java.lang.String")
                    .usingEqStrings(java.util.List.of(typeName))));
            // 458 has two XDTUserDict classes: the User model (X.02gr, ~570 methods) and the
            // small backing dict it wraps (X.02fp, ~50). Screens hold the model, so take the
            // biggest candidate rather than whichever DexKit happens to list first.
            String best = null;
            int bestMethods = -1;
            for (MethodData md : hits) {
                String cls = md.getClassName();
                try {
                    Class<?> c = Class.forName(cls, false, cl);
                    if (Modifier.isAbstract(c.getModifiers()) || c.isInterface()) continue;
                    if (accessor(c) == null) continue;
                    int n = c.getDeclaredMethods().length;
                    if (n > bestMethods) {
                        best = cls;
                        bestMethods = n;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (best != null) return best;
        } catch (Throwable t) {
            ModuleLog.line("(IE|Models) ⚠️ findByTypeName " + typeName + ": " + t.getMessage());
        }
        return null;
    }

    private static boolean loads(String cls, ClassLoader cl) {
        try {
            Class.forName(cls, false, cl);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * The generic field accessor on {@code c} (or a superclass): a public, non-static, non-synthetic
     * {@code Object name(int)} method. Null when the class has none or more than one candidate.
     */
    @Nullable
    public static Method accessor(Class<?> c) {
        Object v = ACCESSORS.get(c);
        if (v == null) {
            v = findAccessor(c);
            ACCESSORS.put(c, v == null ? NONE : v);
        }
        return v == NONE ? null : (Method) v;
    }

    @Nullable
    private static Method findAccessor(Class<?> c) {
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            Method pick = null;
            int n = 0;
            for (Method m : k.getDeclaredMethods()) {
                int mod = m.getModifiers();
                if (Modifier.isStatic(mod) || m.isSynthetic() || m.isBridge()) continue;
                if (m.getReturnType() != Object.class) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1 || p[0] != int.class) continue;
                pick = m;
                n++;
            }
            if (n == 1) {
                pick.setAccessible(true);
                return pick;
            }
            if (n > 1) return null;
        }
        return null;
    }

    /** JSON field value by name, or null when the model has no accessor or the field is unset. */
    @Nullable
    public static Object read(@Nullable Object model, String field) {
        if (model == null) return null;
        Method m = accessor(model.getClass());
        if (m == null) return null;
        try {
            return m.invoke(model, field.hashCode());
        } catch (Throwable t) {
            return null;
        }
    }

    /** True when the model exposes the generic accessor ({@link #read} can work on it). */
    public static boolean readable(@Nullable Object model) {
        return model != null && accessor(model.getClass()) != null;
    }

    @Nullable
    public static Boolean readBoolean(@Nullable Object model, String field) {
        Object v = read(model, field);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    @Nullable
    public static String readString(@Nullable Object model, String field) {
        Object v = read(model, field);
        if (v instanceof String) return (String) v;
        if (v instanceof Enum) return v.toString();
        return null;
    }

    @Nullable
    public static List<?> readList(@Nullable Object model, String field) {
        Object v = read(model, field);
        return v instanceof List ? (List<?>) v : null;
    }

    /** Test hook. */
    static void resetForTest() {
        userClass = null;
        resolved = false;
        ACCESSORS.clear();
    }
}
