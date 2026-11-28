package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.BuildConfig;

public class ReleaseVersionTest {

    @Test
    public void versionIs091AboveOriginalBeta() {
        assertEquals("0.9.1", BuildConfig.VERSION_NAME);
        assertEquals(24, BuildConfig.VERSION_CODE);
        assertTrue("must update over the original 0.8.3 beta (code 21)", BuildConfig.VERSION_CODE > 21);
    }

    @Test
    public void targetsMatchTheCompatibilityLists() {
        // Release compatibility target; stable = newest checked stable.
        assertEquals("458.0.0.0.67", ExportBrand.TARGET_INSTAGRAM);
        assertEquals("457.0.0.55.73", ExportBrand.STABLE_INSTAGRAM);
        assertEquals(ExportBrand.TARGET_INSTAGRAM, IgCompat.CHECKED_OFFLINE[0]);
        assertTrue(java.util.Arrays.asList(IgCompat.CHECKED_OFFLINE).contains(ExportBrand.STABLE_INSTAGRAM));
        // No build of this release has been run on a device yet; don't claim one.
        assertEquals(0, IgCompat.DEVICE_TESTED.length);
        assertEquals("https://www.apkmirror.com/apk/instagram/instagram-instagram/instagram-457-0-0-55-73-release/",
                ExportBrand.apkMirrorUrl(ExportBrand.STABLE_INSTAGRAM));
        assertEquals("https://www.apkmirror.com/apk/instagram/instagram-instagram/instagram-458-0-0-0-67-release/",
                ExportBrand.apkMirrorUrl(ExportBrand.TARGET_INSTAGRAM));
    }

    @Test
    public void releaseHeadingIsVersionedOrHasVerifiedInstagramTarget() throws Exception {
        java.nio.file.Path p = java.nio.file.Paths.get("CHANGELOG.md");
        if (!java.nio.file.Files.exists(p)) p = java.nio.file.Paths.get("..", "CHANGELOG.md");
        String first = null;
        for (String l : java.nio.file.Files.readAllLines(p, java.nio.charset.StandardCharsets.UTF_8)) {
            // Keep a Changelog's "## Unreleased" section names no target build; check the first release.
            if (l.startsWith("## ") && !l.trim().equalsIgnoreCase("## Unreleased")) { first = l; break; }
        }
        assertTrue("no release heading", first != null);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("for Instagram (\\S+)").matcher(first);
        if (m.find()) {
            assertTrue("CHANGELOG target " + m.group(1) + " must be in IgCompat.CHECKED_OFFLINE or DEVICE_TESTED",
                    IgCompat.isVerified(IgCompat.classify(m.group(1))));
        } else {
            assertTrue("release heading must identify its Instagram target or be a dated version heading",
                    first.matches("## \\d+\\.\\d+\\.\\d+ - \\d{4}-\\d{2}-\\d{2}"));
        }
    }

    @Test
    public void compatibilityMatrixListsEveryCheckedBuild() throws Exception {
        java.nio.file.Path p = java.nio.file.Paths.get("docs", "COMPATIBILITY_MATRIX.md");
        if (!java.nio.file.Files.exists(p)) p = java.nio.file.Paths.get("..", "docs", "COMPATIBILITY_MATRIX.md");
        String doc = new String(java.nio.file.Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8);
        for (String v : IgCompat.CHECKED_OFFLINE) assertTrue(v + " missing from the matrix", doc.contains(v));
        for (String v : IgCompat.DEVICE_TESTED) assertTrue(v + " missing from the matrix", doc.contains(v));
    }
}
