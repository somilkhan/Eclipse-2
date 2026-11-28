package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.utils.core.IgCompat.Status;

public class IgCompatTest {
    private static final String TARGET = ExportBrand.TARGET_INSTAGRAM;   // 458.0.0.0.67, alpha
    private static final String STABLE = ExportBrand.STABLE_INSTAGRAM;   // 457.0.0.55.73
    private static final int MAX = Math.max(IgCompat.major(TARGET), IgCompat.major(STABLE));
    private static final int MIN = Math.min(IgCompat.major(TARGET), IgCompat.major(STABLE));

    @Test public void checkedBuildsAreCheckedOfflineNotTested() {
        assertEquals(Status.CHECKED_OFFLINE, IgCompat.classify(TARGET));
        assertEquals(Status.CHECKED_OFFLINE, IgCompat.classify(" " + STABLE + " "));
        assertEquals(Status.CHECKED_OFFLINE, IgCompat.classify("458.0.0.0.67"));
        assertEquals(Status.CHECKED_OFFLINE, IgCompat.classify("457.0.0.55.73"));
        assertTrue(IgCompat.isVerified(Status.CHECKED_OFFLINE));
        assertTrue(IgCompat.isVerified(Status.TESTED));
        assertFalse(IgCompat.isVerified(Status.UNTESTED_BUILD));
    }

    @Test public void sameMajorOtherBuild() {
        // 458.0.0.0.72 is the newest public alpha (Nov 2026) but has no hook-verify report.
        assertEquals(Status.UNTESTED_BUILD, IgCompat.classify("458.0.0.0.72"));
        assertEquals(Status.UNTESTED_BUILD, IgCompat.classify("457.0.0.0.26"));
        assertEquals(Status.UNTESTED_BUILD, IgCompat.classify(MAX + ".1.0.9.99"));
    }

    @Test public void newerStableAndAlpha() {
        assertEquals(Status.NEWER, IgCompat.classify((MAX + 1) + ".0.0.40.71"));
        assertEquals(Status.NEWER_ALPHA, IgCompat.classify((MAX + 1) + ".0.0.0.67"));
    }

    @Test public void older() {
        assertEquals(Status.OLDER, IgCompat.classify((MIN - 3) + ".0.0.51.72"));
    }

    @Test public void unparseable() {
        assertEquals(Status.UNKNOWN, IgCompat.classify(null));
        assertEquals(Status.UNKNOWN, IgCompat.classify("beta"));
    }

    @Test public void alphaUsersAreOfferedTheCheckedAlpha() {
        // Regression: 0.9.0 told 458 alpha users to leave the testing programme and install 457.
        assertEquals("458.0.0.0.67", IgCompat.recommendedFor("458.0.0.0.72"));
        assertEquals("458.0.0.0.67", IgCompat.recommendedFor((MAX + 1) + ".0.0.0.5"));
        assertEquals("458.0.0.0.67", IgCompat.recommendedFor("458.0.0.0.67"));
    }

    @Test public void stableUsersAreOfferedTheCheckedStable() {
        assertEquals("457.0.0.55.73", IgCompat.recommendedFor("457.0.0.12.34"));
        assertEquals("457.0.0.55.73", IgCompat.recommendedFor((MAX + 1) + ".0.0.40.71"));
        assertEquals("457.0.0.55.73", IgCompat.recommendedFor("455.0.0.51.72"));
    }

    @Test public void sameMajorWinsOverChannel() {
        // A 457 alpha has no checked 457 alpha; the checked 457 stable shares its major.
        assertEquals("457.0.0.55.73", IgCompat.recommendedFor("457.0.0.0.26"));
        // A 458 stable has no checked 458 stable; the checked 458 alpha shares its major.
        assertEquals("458.0.0.0.67", IgCompat.recommendedFor("458.0.0.40.71"));
    }

    @Test public void unknownVersionGetsTheCheckedStable() {
        assertEquals(STABLE, IgCompat.recommendedFor(null));
    }

    @Test public void evidenceSummaryIsHonest() {
        String s = IgCompat.evidenceSummary();
        assertEquals("458.0.0.0.67, 457.0.0.55.73 (offline hook check; no device test)", s);
        assertFalse(s, s.contains("device-tested"));
    }

    @Test public void versionCompare() {
        assertTrue(IgCompat.compare("458.0.0.0.72", "458.0.0.0.67") > 0);
        assertTrue(IgCompat.compare("457.0.0.55.73", "458.0.0.0.1") < 0);
        assertEquals(0, IgCompat.compare("1.2", "1.2.0"));
    }

    @Test public void alphaPattern() {
        assertTrue(IgCompat.isAlpha("458.0.0.0.67"));
        assertFalse(IgCompat.isAlpha("457.0.0.55.73"));
        assertFalse(IgCompat.isAlpha("456.0.0.24.78"));
    }
}
