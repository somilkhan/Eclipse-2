package io.github.somilkhan.instaeclipse2.Xposed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LegacyModuleCheckTest {
    private static final long START = 1_000_000_000L;

    @Test public void missingFileIsNotActive() {
        assertFalse(LegacyModuleCheck.isActive(false, START + 5000, START));
    }

    @Test public void staleFileFromEarlierSessionIsNotActive() {
        assertFalse(LegacyModuleCheck.isActive(true, START - 60_000, START));
    }

    @Test public void fileWrittenThisSessionIsActive() {
        assertTrue(LegacyModuleCheck.isActive(true, START + 3000, START));
    }

    @Test public void zeroTimestampIsNotActive() {
        assertFalse(LegacyModuleCheck.isActive(true, 0, START));
    }
}
