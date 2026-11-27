package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.utils.core.IgCompat.Status;

public class IgCompatTest {
    private static final String T = ExportBrand.TESTED_INSTAGRAM;
    private static final int MAJOR = Integer.parseInt(T.substring(0, T.indexOf('.')));

    @Test public void exactTestedVersion() {
        assertEquals(Status.TESTED, IgCompat.classify(T));
        assertEquals(Status.TESTED, IgCompat.classify(" " + T + " "));
    }

    @Test public void sameMajorOtherBuild() {
        assertEquals(Status.UNTESTED_BUILD, IgCompat.classify(MAJOR + ".1.0.9.99"));
    }

    @Test public void newerStableAndAlpha() {
        assertEquals(Status.NEWER, IgCompat.classify((MAJOR + 1) + ".0.0.40.71"));
        assertEquals(Status.NEWER_ALPHA, IgCompat.classify((MAJOR + 1) + ".0.0.0.67"));
    }

    @Test public void older() {
        assertEquals(Status.OLDER, IgCompat.classify((MAJOR - 3) + ".0.0.51.72"));
    }

    @Test public void unparseable() {
        assertEquals(Status.UNKNOWN, IgCompat.classify(null));
        assertEquals(Status.UNKNOWN, IgCompat.classify("beta"));
    }

    @Test public void alphaPattern() {
        assertTrue(IgCompat.isAlpha("458.0.0.0.67"));
        assertFalse(IgCompat.isAlpha("457.0.0.55.73"));
        assertFalse(IgCompat.isAlpha("456.0.0.24.78"));
    }
}
