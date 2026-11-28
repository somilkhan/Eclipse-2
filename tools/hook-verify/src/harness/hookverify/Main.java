package hookverify;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * hook-verify entry point. See tools/hook-verify/README.md.
 *
 * Options (system properties):
 *   hookverify.apk        Instagram APK (required)
 *   hookverify.dexkit     path to the host libdexkit.so (required)
 *   hookverify.moduleCp   module runtime classpath incl. runner classes (required, ':'-separated)
 *   hookverify.moduleJava path to app/src/main/java/io/github/somilkhan/instaeclipse2/Xposed/Module.java (required)
 *   hookverify.report     output markdown report (default build/hook-verify/report.md)
 *   hookverify.filter     regex on step keys to run only some steps
 *   hookverify.timeout    per-step timeout seconds (default 300)
 *   hookverify.echo       print every hook/log line live
 */
public final class Main {
    /** Log markers that mean "this lookup or hook did not resolve". */
    static final Pattern FAIL = Pattern.compile(
            "❌|⚠|not found|NOT FOUND|notfound|no match|No match|failed|Failed|FAILED|missing|Missing|unresolved|couldn't|could not|Cannot|cannot find|NoSuch|ClassNotFound|skipped:", 0);

    /**
     * Failure lines caused by the host JVM, not by a lookup: Instagram code is stubbed and
     * android.jar is a mockable jar, so constructing IG objects or reading framework statics
     * fails offline although the lookup itself resolved. Such steps are still counted as
     * PARTIAL/FAIL but tagged "(harness)" so nobody mistakes them for device failures, and
     * nobody mistakes them for passes either.
     */
    static final Pattern HARNESS = Pattern.compile("hook-verify stub|CREATOR\"? is null|Stub!", 0);

    static boolean harnessOnly(Recorder.Step s) {
        List<String> bad = s.logs.stream().filter(l -> FAIL.matcher(l).find()).toList();
        return !bad.isEmpty() && bad.stream().allMatch(l -> HARNESS.matcher(l).find());
    }

    public static void main(String[] a) throws Exception {
        String apk = req("hookverify.apk");
        String lib = req("hookverify.dexkit");
        String moduleCp = req("hookverify.moduleCp");
        String moduleJava = req("hookverify.moduleJava");
        String report = System.getProperty("hookverify.report", "build/hook-verify/report.md");
        String filter = System.getProperty("hookverify.filter", "");
        long timeout = Long.getLong("hookverify.timeout", 300);

        long t0 = System.currentTimeMillis();
        IgClassLoader ig = new IgClassLoader(new File(apk), Main.class.getClassLoader());
        System.out.println("[hook-verify] indexed " + ig.classCount() + " Instagram classes in " + (System.currentTimeMillis() - t0) + " ms");

        List<URL> urls = new ArrayList<>();
        for (String p : moduleCp.split(File.pathSeparator)) if (!p.isEmpty()) urls.add(new File(p).toURI().toURL());
        URLClassLoader module = new URLClassLoader(urls.toArray(new URL[0]), Main.class.getClassLoader());
        Class<?> runner = Class.forName("hookverify.runner.Runner", true, module);
        @SuppressWarnings("unchecked")
        List<Recorder.Step> steps = (List<Recorder.Step>) runner.getMethod("run", ClassLoader.class, String.class, String.class,
                String.class, String.class, String.class, long.class).invoke(null, ig, apk, lib, moduleJava, apk, filter, timeout);

        String md = render(steps, apk);
        Path out = Path.of(report);
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.writeString(out, md, StandardCharsets.UTF_8);
        System.out.println();
        System.out.println(summaryTable(steps));
        System.out.println("[hook-verify] report written to " + out.toAbsolutePath());
        System.exit(0);
    }

    private static String req(String k) {
        String v = System.getProperty(k);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("missing -D" + k);
        return v;
    }

    public static String status(Recorder.Step s) {
        if (s.error != null) return "ERROR";
        long bad = s.logs.stream().filter(l -> FAIL.matcher(l).find()).count();
        if (bad > 0 && s.hooks.isEmpty()) return "FAIL";
        if (bad > 0) return "PARTIAL";
        if (s.hooks.isEmpty()) return "NO-HOOK";
        return "OK";
    }

    static String summaryTable(List<Recorder.Step> steps) {
        StringBuilder sb = new StringBuilder();
        int ok = 0, partial = 0, fail = 0, nohook = 0, err = 0;
        for (Recorder.Step s : steps) {
            String st = status(s);
            switch (st) { case "OK": ok++; break; case "PARTIAL": partial++; break; case "FAIL": fail++; break; case "NO-HOOK": nohook++; break; default: err++; }
            sb.append(String.format(Locale.ROOT, "%-8s %-32s hooks=%-3d %6d ms%s%n", st, s.key, s.hooks.size(), s.millis,
                    harnessOnly(s) ? "  (harness)" : ""));
        }
        sb.append(String.format(Locale.ROOT, "TOTAL: %d steps  OK=%d PARTIAL=%d FAIL=%d NO-HOOK=%d ERROR=%d%n",
                steps.size(), ok, partial, fail, nohook, err));
        return sb.toString();
    }

    static String render(List<Recorder.Step> steps, String apk) {
        StringBuilder sb = new StringBuilder();
        sb.append("# hook-verify report\n\n");
        String ver = System.getProperty("hookverify.apkVersion", "");
        sb.append("APK: `").append(new File(apk).getName()).append("`");
        if (!ver.isEmpty()) sb.append(" — Instagram ").append(ver);
        sb.append("\n\nResults hold for this exact build only; a device running another build of the same ")
          .append("major version can resolve different classes.\n\n");
        sb.append("Status legend: **OK** = hooks installed and no failure lines; **PARTIAL** = some hooks installed but ")
          .append("at least one sub-lookup logged a failure; **FAIL** = failure logged and nothing hooked; ")
          .append("**NO-HOOK** = nothing hooked and nothing logged (usually a hook that installs lazily at runtime, ")
          .append("or one that only hooks framework classes once an Activity exists); **ERROR** = install threw. ")
          .append("**(harness)** = every failure line comes from the offline harness (stubbed Instagram code or ")
          .append("mockable android.jar), not from a lookup; confirm on a device.\n\n");
        sb.append("```\n").append(summaryTable(steps)).append("```\n\n");
        if (!Recorder.features.isEmpty()) {
            int h = 0, b = 0, q = 0;
            for (String[] f : Recorder.features) { if ("HOOKED".equals(f[1])) h++; else if ("BROKEN".equals(f[1])) b++; else q++; }
            sb.append("## Feature status (FeatureStatusTracker)\n\n")
              .append("Every feature registered by `FeatureManager.refreshFeatureStatus()` with all flags on, after all steps. ")
              .append("**HOOKED** = the feature reported its hook; **BROKEN** = it reported a failed lookup (shown red in ")
              .append("the load toast); **SILENT** = it reported neither, so on a device the load toast cannot say whether it ")
              .append("works. SILENT is not proof of a failure: runtime-only hooks report later.\n\n")
              .append(String.format(Locale.ROOT, "HOOKED=%d BROKEN=%d SILENT=%d%n%n", h, b, q))
              .append("| Feature key | State |\n|---|---|\n");
            for (String[] f : Recorder.features) sb.append("| `").append(f[0]).append("` | ").append(f[1]).append(" |\n");
            sb.append('\n');
        }
        for (Recorder.Step s : steps) {
            sb.append("## ").append(status(s)).append(" — ").append(s.key).append("\n\n");
            sb.append("`").append(s.call).append("` (").append(s.millis).append(" ms)\n\n");
            if (s.error != null) {
                sb.append("Error:\n```\n").append(trace(s.error)).append("```\n\n");
            }
            if (!s.hooks.isEmpty()) {
                sb.append("Hooked (").append(s.hooks.size()).append("):\n");
                for (String h : s.hooks) sb.append("- `").append(h).append("`\n");
                sb.append('\n');
            }
            if (!s.logs.isEmpty()) {
                sb.append("Log:\n```\n");
                for (String l : s.logs) sb.append(FAIL.matcher(l).find() ? "!! " : "   ").append(l).append('\n');
                sb.append("```\n\n");
            }
        }
        return sb.toString();
    }

    private static String trace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (Throwable c = t; c != null && depth < 4; c = c.getCause(), depth++) {
            sb.append(depth == 0 ? "" : "Caused by: ").append(c).append('\n');
            StackTraceElement[] st = c.getStackTrace();
            for (int i = 0; i < Math.min(8, st.length); i++) sb.append("    at ").append(st[i]).append('\n');
        }
        return sb.toString();
    }
}
