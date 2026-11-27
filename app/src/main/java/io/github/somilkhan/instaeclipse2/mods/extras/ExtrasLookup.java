package io.github.somilkhan.instaeclipse2.mods.extras;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;

import io.github.somilkhan.instaeclipse2.utils.core.DexKitCache;
import io.github.somilkhan.instaeclipse2.utils.core.StringTables;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * DexKit lookups shared by the "Extras" hooks: resolve once, cache the result per IG version,
 * and remember misses so a feature IG removed doesn't cost a DexKit scan on every launch.
 */
final class ExtrasLookup {

    interface Finder {
        Method find() throws Throwable;
    }

    private ExtrasLookup() {}

    /** Cached method for {@code key}; runs {@code finder} on a cache miss. Null when not found. */
    static Method method(String key, ClassLoader cl, Finder finder) {
        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod(key, cl);
            if (cached != null) return cached;
            if ("missing".equals(DexKitCache.loadString(key))) return null;
        }
        Method m = null;
        try {
            m = finder.find();
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Extras): " + key + " lookup failed: " + t);
        }
        if (m != null) {
            DexKitCache.saveMethod(key, m);
        } else {
            DexKitCache.saveString(key, "missing");
            ModuleLog.line("(InstaEclipse | Extras): " + key + " not found in this Instagram version");
        }
        return m;
    }

    /**
     * Methods whose body references every given string — inline (older builds) or through a
     * 457-style shared string table ({@link StringTables#findMethodUsingStrings}).
     */
    static List<MethodData> byStrings(DexKitBridge bridge, String... strings) {
        List<MethodData> direct = StringTables.withoutTables(
                bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create().usingStrings(strings))));
        if (direct.isEmpty()) return StringTables.findMethodUsingStrings(bridge, null, strings);
        if (strings.length != 1) return direct;
        // 457 keeps some inline copies of a string and routes others through the shared table
        // (e.g. "account_switcher_max_limit_reached": inline in a lambda, via the table in the
        // add-account click handler), so append the table users after the direct ones.
        java.util.Map<String, MethodData> all = new java.util.LinkedHashMap<>();
        for (MethodData m : direct) all.put(m.getDescriptor(), m);
        for (MethodData m : StringTables.findTableUsers(bridge, strings[0])) all.putIfAbsent(m.getDescriptor(), m);
        return new java.util.ArrayList<>(all.values());
    }

    /** First method invoked by {@code owner} that is static and satisfies {@code test}. */
    static Method firstStaticInvoke(MethodData owner, ClassLoader cl, Predicate<Method> test) {
        for (MethodData inv : owner.getInvokes()) {
            if (!inv.isMethod()) continue;
            try {
                Method m = inv.getMethodInstance(cl);
                if (Modifier.isStatic(m.getModifiers()) && test.test(m)) return m;
            } catch (Throwable ignored) {
                // unresolvable framework/synthetic reference
            }
        }
        return null;
    }

    static boolean takesSession(Method m) {
        Class<?>[] p = m.getParameterTypes();
        return p.length == 1 && p[0].getName().equals("com.instagram.common.session.UserSession");
    }
}
