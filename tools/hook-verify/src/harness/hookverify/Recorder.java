package hookverify;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects, per install step, every hook request (from the XposedBridge stand-in) and every log
 * line (from the android.util.Log stand-in). Steps run sequentially on one thread; work a step
 * starts on other threads is attributed to whichever step is current when it reports.
 */
public final class Recorder {
    public static final class Step {
        public final String key;
        public final String call;
        public final List<String> hooks = new ArrayList<>();
        public final List<String> logs = new ArrayList<>();
        public Throwable error;
        public long millis;
        Step(String key, String call) { this.key = key; this.call = call; }
    }

    private static final Map<String, Step> steps = new LinkedHashMap<>();
    private static volatile Step current;
    public static volatile boolean echo = Boolean.getBoolean("hookverify.echo");

    private Recorder() {}

    public static synchronized Step begin(String key, String call) {
        Step s = new Step(key, call);
        steps.put(key + "#" + steps.size(), s);
        current = s;
        return s;
    }

    public static synchronized void end() { current = null; }

    public static synchronized List<Step> steps() { return new ArrayList<>(steps.values()); }

    public static void hook(Member m) {
        String d = describe(m);
        synchronized (Recorder.class) {
            if (current != null) current.hooks.add(d);
        }
        if (echo) System.out.println("    [hook] " + d);
    }

    public static void log(String line) {
        synchronized (Recorder.class) {
            if (current != null) current.logs.add(line);
        }
        if (echo) System.out.println("    [log] " + line);
    }

    public static String describe(Member m) {
        StringBuilder sb = new StringBuilder(m.getDeclaringClass().getName()).append('#');
        Class<?>[] p;
        if (m instanceof Method) { sb.append(m.getName()); p = ((Method) m).getParameterTypes(); }
        else { sb.append("<init>"); p = ((Constructor<?>) m).getParameterTypes(); }
        sb.append('(');
        for (int i = 0; i < p.length; i++) { if (i > 0) sb.append(','); sb.append(p[i].getTypeName()); }
        sb.append(')');
        if (m instanceof Method) sb.append(((Method) m).getReturnType().getTypeName());
        return sb.toString();
    }
}
