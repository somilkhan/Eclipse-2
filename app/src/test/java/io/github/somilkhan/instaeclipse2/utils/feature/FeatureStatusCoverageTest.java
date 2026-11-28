package io.github.somilkhan.instaeclipse2.utils.feature;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every feature the load toast lists (FeatureManager / ExtrasFeatures) must report its hook with
 * FeatureStatusTracker.setHooked somewhere. Without that the toast can never tell the user whether
 * it works, so a broken lookup goes unnoticed. hook-verify's "Feature status" section checks the
 * same thing at runtime against a real APK.
 */
public class FeatureStatusCoverageTest {

    private static File src(String rel) {
        File f = new File("src/main/java/io/github/somilkhan/instaeclipse2/" + rel);
        return f.exists() ? f : new File("app/" + f.getPath());
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    static Set<String> registeredKeys(String manager, String extras) {
        Set<String> keys = new TreeSet<>();
        Matcher m = Pattern.compile("setEnabled\\(\\s*\"(\\w+)\"").matcher(manager);
        while (m.find()) keys.add(m.group(1));
        m = Pattern.compile("\\breg\\([^;]*?,\\s*\"(\\w+)\"").matcher(manager);
        while (m.find()) keys.add(m.group(1));
        m = Pattern.compile("new Entry\\(\\s*\"(\\w+)\"").matcher(extras);
        while (m.find()) keys.add(m.group(1));
        return keys;
    }

    @Test public void everyRegisteredFeatureReportsItsHook() throws IOException {
        Set<String> keys = registeredKeys(read(src("utils/feature/FeatureManager.java")),
                read(src("utils/feature/ExtrasFeatures.java")));
        assertTrue("parser found too few keys: " + keys, keys.size() > 50);

        List<String> hookedLines = new ArrayList<>();
        Path root = src("").toPath();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (line.contains("setHooked(")) hookedLines.add(line);
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (String k : keys) {
            String lit = "\"" + k + "\"";
            if (hookedLines.stream().noneMatch(l -> l.contains(lit))) missing.add(k);
        }
        assertTrue("features with no setHooked(\"key\") call (the load toast can't report them): " + missing,
                missing.isEmpty());
    }
}
