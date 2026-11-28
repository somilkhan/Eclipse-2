package io.github.somilkhan.instaeclipse2.Xposed;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Guards hook isolation in Module.java: a try block must not hold more than one skip-gated
 * install, because one throw would silently skip the hooks after it. Use isolatedInstall(...)
 * for each hook instead. Also checks that every install line uses a form tools/hook-verify parses.
 */
public class ModuleInstallIsolationTest {
    private static final Pattern GATED = Pattern.compile("^if \\(!skip\\(\"[^\"]+\"\\)\\)\\s*.+;\\s*(//.*)?$");
    private static final Pattern ISOLATED = Pattern.compile("^isolatedInstall\\(\"[^\"]+\",\\s*\\(\\)\\s*->\\s*.+\\);\\s*(//.*)?$");

    private static List<String> moduleLines() throws Exception {
        File f = new File("src/main/java/io/github/somilkhan/instaeclipse2/Xposed/Module.java");
        if (!f.exists()) f = new File("app/" + f.getPath());
        return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
    }

    @Test
    public void noTryBlockHoldsTwoGatedInstalls() throws Exception {
        List<String> lines = moduleLines();
        List<String> bad = new ArrayList<>();
        int depthTry = -1, gated = 0, start = 0;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i).trim();
            if (l.equals("try {")) { depthTry = i; gated = 0; start = i + 1; continue; }
            if (depthTry >= 0 && l.startsWith("} catch")) {
                if (gated > 1) bad.add("Module.java:" + start + " (" + gated + " installs)");
                depthTry = -1;
                continue;
            }
            if (depthTry >= 0 && GATED.matcher(l).matches()) gated++;
        }
        assertTrue("try blocks sharing several hook installs: " + bad, bad.isEmpty());
    }

    @Test
    public void everyInstallLineIsParseableByHookVerify() throws Exception {
        List<String> bad = new ArrayList<>();
        List<String> lines = moduleLines();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i).trim();
            boolean looksLikeInstall = l.contains("if (!skip(\"") || l.startsWith("isolatedInstall(\"");
            if (looksLikeInstall && !GATED.matcher(l).matches() && !ISOLATED.matcher(l).matches()) {
                bad.add("Module.java:" + (i + 1) + ": " + l);
            }
        }
        assertTrue("install lines hook-verify can't parse: " + bad, bad.isEmpty());
    }
}
