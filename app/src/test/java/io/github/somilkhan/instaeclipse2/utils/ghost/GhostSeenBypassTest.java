package io.github.somilkhan.instaeclipse2.utils.ghost;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

/** ENG-13: Mark as read must never write the user's Ghost seen setting. */
public class GhostSeenBypassTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private LongSupplier savedClock;
    private boolean savedSeen;

    @Before public void setUp() {
        savedClock = GhostSeenBypass.clock;
        savedSeen = FeatureFlags.isGhostSeen;
        GhostSeenBypass.clock = now::get;
        GhostSeenBypass.resetForTest();
    }

    @After public void tearDown() {
        GhostSeenBypass.clock = savedClock;
        GhostSeenBypass.resetForTest();
        FeatureFlags.isGhostSeen = savedSeen;
    }

    @Test public void windowLetsSeenThroughWithoutTouchingTheSetting() {
        FeatureFlags.isGhostSeen = true;
        assertTrue(GhostSeenBypass.blocksSeen());
        GhostSeenBypass.Window w = GhostSeenBypass.begin();
        assertFalse(GhostSeenBypass.blocksSeen());
        assertTrue("setting untouched", FeatureFlags.isGhostSeen);
        w.close();
        assertTrue(GhostSeenBypass.blocksSeen());
    }

    @Test public void ghostSeenOffStaysOffAfterTheButton() {
        // Regression: the old button forced isGhostSeen = true when its callback ran.
        FeatureFlags.isGhostSeen = false;
        GhostSeenBypass.begin().close();
        assertFalse(FeatureFlags.isGhostSeen);
        assertFalse(GhostSeenBypass.blocksSeen());
    }

    @Test public void toggleDuringWindowIsKept() {
        FeatureFlags.isGhostSeen = true;
        GhostSeenBypass.Window w = GhostSeenBypass.begin();
        FeatureFlags.isGhostSeen = false; // quick toggle while the scroll runs
        w.close();
        assertFalse(FeatureFlags.isGhostSeen);
    }

    @Test public void lostCloseExpires() {
        FeatureFlags.isGhostSeen = true;
        GhostSeenBypass.begin(); // never closed (view detached before the posted callback)
        now.addAndGet(GhostSeenBypass.MAX_MS - 1);
        assertFalse(GhostSeenBypass.blocksSeen());
        now.addAndGet(1);
        assertTrue(GhostSeenBypass.blocksSeen());
    }

    @Test public void overlappingWindowsAndDoubleClose() {
        FeatureFlags.isGhostSeen = true;
        GhostSeenBypass.Window a = GhostSeenBypass.begin();
        GhostSeenBypass.Window b = GhostSeenBypass.begin();
        a.close();
        a.close(); // idempotent: must not close b's window
        assertFalse(GhostSeenBypass.blocksSeen());
        b.close();
        assertTrue(GhostSeenBypass.blocksSeen());
    }
}
