package hookverify.runner;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import hookverify.Recorder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs inside the module's class loader. Reads the install sequence straight out of
 * Xposed/Module.java (every {@code if (!skip("Key")) ...;} statement), then executes each call
 * against the real DexKit bridge and the synthesised Instagram class loader.
 */
public final class Runner {
    public static final class StepSpec {
        public final String key, expr; public final int line;
        StepSpec(String key, String expr, int line) { this.key = key; this.expr = expr; this.line = line; }
    }

    private static final Pattern SKIP = Pattern.compile("if \\(!skip\\(\"([^\"]+)\"\\)\\)\\s*(.+?);\\s*(//.*)?$");
    private static final Pattern NEW_CALL = Pattern.compile("^new ([\\w.]+)\\(([^)]*)\\)\\.(\\w+)\\(([^)]*)\\)$");
    private static final Pattern CALL = Pattern.compile("^([\\w.]+)\\.(\\w+)\\(([^)]*)\\)$");

    private final ClassLoader moduleLoader;
    private final ClassLoader igLoader;
    private final Object bridge;
    private final String apk;
    private final String moduleSource;
    private final Map<String, String> imports = new HashMap<>();
    private final Map<String, String> varTypes = new HashMap<>();

    private Runner(ClassLoader igLoader, Object bridge, String apk, String moduleSource) {
        this.moduleLoader = Runner.class.getClassLoader();
        this.igLoader = igLoader;
        this.bridge = bridge;
        this.apk = apk;
        this.moduleSource = moduleSource;
    }

    /** Entry point called reflectively by hookverify.Main. */
    public static List<Recorder.Step> run(ClassLoader igLoader, String apk, String dexkitLib, String moduleJava,
                                          String moduleApk, String filter, long timeoutSec) throws Exception {
        System.load(dexkitLib); // must be called from the loader that owns DexKitBridge
        long t0 = System.currentTimeMillis();
        Class<?> bridgeCls = Class.forName("org.luckypray.dexkit.DexKitBridge");
        Object bridge = bridgeCls.getMethod("create", String.class).invoke(null, apk);
        System.out.println("[hook-verify] DexKit bridge ready in " + (System.currentTimeMillis() - t0) + " ms");
        Runner r = new Runner(igLoader, bridge, apk, moduleApk);
        r.prepareModuleState();
        List<StepSpec> specs = r.parse(Files.readAllLines(Path.of(moduleJava), StandardCharsets.UTF_8));
        ExecutorService ex = Executors.newSingleThreadExecutor(rn -> {
            Thread t = new Thread(rn, "hook-verify-step");
            t.setDaemon(true);
            return t;
        });
        for (StepSpec s : specs) {
            if (filter != null && !filter.isEmpty() && !java.util.regex.Pattern.compile(filter).matcher(s.key).find()) continue;
            Recorder.Step step = Recorder.begin(s.key, s.expr);
            System.out.println("[hook-verify] " + s.key + "  <-  " + s.expr);
            long st = System.currentTimeMillis();
            Future<?> f = ex.submit(() -> { r.execute(s); return null; });
            try {
                f.get(timeoutSec, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                step.error = e.getCause() instanceof java.lang.reflect.InvocationTargetException
                        ? e.getCause().getCause() : e.getCause();
            } catch (java.util.concurrent.TimeoutException e) {
                step.error = new RuntimeException("timed out after " + timeoutSec + " s");
                f.cancel(true);
                ex.shutdownNow();
                ex = Executors.newSingleThreadExecutor();
            }
            // give fire-and-forget background lookups a moment to report into this step
            Thread.sleep(50);
            step.millis = System.currentTimeMillis() - st;
            Recorder.end();
        }
        ex.shutdownNow();
        return Recorder.steps();
    }

    /** Flags on (so every feature's install path runs), statics the hooks read from Module. */
    private void prepareModuleState() throws Exception {
        Class<?> flags = Class.forName("ps.reso.instaeclipse.utils.feature.FeatureFlags", true, moduleLoader);
        int on = 0;
        for (Field f : flags.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
            if (f.getType() == boolean.class) { f.setAccessible(true); f.setBoolean(null, true); on++; }
        }
        System.out.println("[hook-verify] FeatureFlags: " + on + " boolean flags forced on");
        Class<?> module = Class.forName("ps.reso.instaeclipse.Xposed.Module", true, moduleLoader);
        setStatic(module, "dexKitBridge", bridge);
        setStatic(module, "hostClassLoader", igLoader);
        setStatic(module, "moduleSourceDir", moduleSource);
        try { // same as Module.handleLoadPackage on the device
            Class.forName("ps.reso.instaeclipse.utils.core.StringTables", true, moduleLoader)
                    .getMethod("init", String.class).invoke(null, apk);
        } catch (ClassNotFoundException ignored) {}
    }

    private static void setStatic(Class<?> c, String name, Object v) {
        try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, v);
        } catch (Throwable ignored) {}
    }

    List<StepSpec> parse(List<String> lines) {
        Pattern imp = Pattern.compile("^import ([\\w.]+)\\.(\\w+);");
        Pattern decl = Pattern.compile("([\\w.]+) (\\w+) = new \\1\\(");
        for (String l : lines) {
            Matcher m = imp.matcher(l.trim());
            if (m.find()) imports.put(m.group(2), m.group(1) + "." + m.group(2));
            Matcher d = decl.matcher(l);
            if (d.find()) varTypes.put(d.group(2), d.group(1));
        }
        List<StepSpec> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = SKIP.matcher(lines.get(i).trim());
            if (m.find()) out.add(new StepSpec(m.group(1), m.group(2).trim(), i + 1));
        }
        return out;
    }

    private String resolveClass(String q) {
        if (varTypes.containsKey(q)) q = varTypes.get(q);
        if (q.contains(".")) return q;
        if (imports.containsKey(q)) return imports.get(q);
        return "ps.reso.instaeclipse.Xposed." + q;
    }

    private Object arg(String a) {
        a = a.trim();
        switch (a) {
            case "dexKitBridge": return bridge;
            case "lpparam.classLoader":
            case "hostClassLoader":
            case "classLoader": return igLoader;
            case "moduleSourceDir": return moduleSource;
            case "lpparam": {
                XC_LoadPackage.LoadPackageParam p = new XC_LoadPackage.LoadPackageParam();
                p.packageName = "com.instagram.android";
                p.processName = "com.instagram.android";
                p.classLoader = igLoader;
                p.isFirstApplication = true;
                return p;
            }
            default: throw new IllegalArgumentException("hook-verify: don't know how to supply argument '" + a + "'");
        }
    }

    private Object[] args(String list) {
        if (list.trim().isEmpty()) return new Object[0];
        String[] parts = list.split(",");
        Object[] out = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = arg(parts[i]);
        return out;
    }

    private static boolean fits(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++)
            if (args[i] != null && !types[i].isInstance(args[i])) return false;
        return true;
    }

    private Object invoke(Class<?> c, Object target, String name, Object[] a) throws Exception {
        for (Class<?> k = c; k != null; k = k.getSuperclass())
            for (Method m : k.getDeclaredMethods())
                if (m.getName().equals(name) && fits(m.getParameterTypes(), a)
                        && (target != null || Modifier.isStatic(m.getModifiers()))) {
                    m.setAccessible(true);
                    return m.invoke(target, a);
                }
        throw new NoSuchMethodException(c.getName() + "#" + name + "/" + a.length);
    }

    private Object construct(Class<?> c, Object[] a) throws Exception {
        for (Constructor<?> k : c.getDeclaredConstructors())
            if (fits(k.getParameterTypes(), a)) { k.setAccessible(true); return k.newInstance(a); }
        throw new NoSuchMethodException(c.getName() + "#<init>/" + a.length);
    }

    private final Map<String, Object> vars = new HashMap<>();

    void execute(StepSpec s) throws Exception {
        Matcher n = NEW_CALL.matcher(s.expr);
        if (n.matches()) {
            Class<?> c = Class.forName(resolveClass(n.group(1)), true, moduleLoader);
            Object o = construct(c, args(n.group(2)));
            invoke(c, o, n.group(3), args(n.group(4)));
            return;
        }
        Matcher m = CALL.matcher(s.expr);
        if (m.matches()) {
            String q = m.group(1);
            if (varTypes.containsKey(q)) { // instance held in a local, e.g. metaAi.install(...)
                Class<?> c = Class.forName(resolveClass(q), true, moduleLoader);
                Object o = vars.computeIfAbsent(q, k -> {
                    try { return construct(c, new Object[0]); } catch (Exception e) { throw new RuntimeException(e); }
                });
                invoke(c, o, m.group(2), args(m.group(3)));
            } else {
                Class<?> c = Class.forName(resolveClass(q), true, moduleLoader);
                invoke(c, null, m.group(2), args(m.group(3)));
            }
            return;
        }
        throw new IllegalArgumentException("hook-verify: unsupported install statement: " + s.expr);
    }

    /** Distinct keys in order, for the "missing from report" check. */
    static Set<String> keys(List<StepSpec> specs) {
        Set<String> s = new LinkedHashSet<>();
        for (StepSpec x : specs) s.add(x.key);
        return s;
    }
}
