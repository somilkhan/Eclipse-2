package io.github.somilkhan.instaeclipse2.utils.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FocusLockTest {

    @Test public void parsesEndTime() {
        assertEquals(0, FocusLock.until(null));
        assertEquals(0, FocusLock.until(""));
        assertEquals(0, FocusLock.until("soon"));
        assertEquals(0, FocusLock.until("-5"));
        assertEquals(1234, FocusLock.until(" 1234 "));
    }

    @Test public void activeUntilTheEndThenExpired() {
        String end = FocusLock.endFor(1_000, 60_000);
        assertEquals("61000", end);
        assertTrue(FocusLock.isActive(end, 60_999));
        assertFalse(FocusLock.isExpired(end, 60_999));
        assertFalse(FocusLock.isActive(end, 61_000));
        assertTrue(FocusLock.isExpired(end, 61_000));
    }

    @Test public void noLockIsNeitherActiveNorExpired() {
        // Permanent Extreme Mode has no end time and must never be released by expiry.
        assertFalse(FocusLock.isActive("", 5));
        assertFalse(FocusLock.isExpired("", Long.MAX_VALUE));
    }

    @Test public void durationsAreIncreasing() {
        long prev = 0;
        for (long d : FocusLock.DURATIONS_MS) {
            assertTrue(d > prev);
            prev = d;
        }
        assertEquals(14, FocusLock.DURATIONS_MS.length);
    }

    @Test public void formatsCompactly() {
        assertEquals("<1m", FocusLock.format(59_000));
        assertEquals("5m", FocusLock.format(5 * 60_000));
        assertEquals("2h", FocusLock.format(2 * 3_600_000L));
        assertEquals("2h 10m", FocusLock.format(2 * 3_600_000L + 10 * 60_000));
        assertEquals("3d", FocusLock.format(3 * 86_400_000L));
        assertEquals("3d 4h", FocusLock.format(3 * 86_400_000L + 4 * 3_600_000L));
    }
}
