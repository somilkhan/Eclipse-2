package io.github.somilkhan.instaeclipse2.utils.pacing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SafePacerTest {

    /** Virtual clock: sleep advances time instantly and records the slices. */
    static final class FakeClock implements SafePacer.Clock {
        long now;
        final List<Long> sleeps = new ArrayList<>();
        Runnable onSleep;

        @Override public long nowMs() { return now; }

        @Override public void sleep(long ms) {
            sleeps.add(ms);
            now += ms;
            if (onSleep != null) onSleep.run();
        }

        long slept() {
            long s = 0;
            for (long v : sleeps) s += v;
            return s;
        }
    }

    /** Random whose nextDouble returns a fixed value (0 → min, ~1 → max). */
    static Random fixed(double v) {
        return new Random() {
            @Override public double nextDouble() { return v; }
        };
    }

    @Test
    public void firstItemStartsImmediately() {
        SafePacer p = new SafePacer(true, new Random(1), new FakeClock());
        assertEquals(0, p.delayBefore(0));
    }

    @Test
    public void shortDelayWithinRange() {
        Random r = new Random(42);
        SafePacer p = new SafePacer(true, r, new FakeClock());
        for (int i = 1; i < 2000; i++) {
            if (i % 8 == 0) continue;
            long d = p.delayBefore(i);
            assertTrue("item " + i + " delay " + d, d >= 700 && d <= 1600);
        }
    }

    @Test
    public void longPauseEveryEighthItem() {
        SafePacer p = new SafePacer(true, new Random(7), new FakeClock());
        for (int i = 8; i < 800; i += 8) {
            long d = p.delayBefore(i);
            assertTrue("item " + i + " delay " + d, d >= 3000 && d <= 6000);
        }
    }

    @Test
    public void boundsAreReachable() {
        assertEquals(700, new SafePacer(true, fixed(0), new FakeClock()).delayBefore(1));
        assertEquals(1600, new SafePacer(true, fixed(0.9999999), new FakeClock()).delayBefore(1));
        assertEquals(3000, new SafePacer(true, fixed(0), new FakeClock()).delayBefore(8));
        assertEquals(6000, new SafePacer(true, fixed(0.9999999), new FakeClock()).delayBefore(16));
    }

    @Test
    public void disabledNeverWaits() {
        FakeClock c = new FakeClock();
        SafePacer p = new SafePacer(false, new Random(1), c);
        for (int i = 0; i < 20; i++) {
            assertEquals(0, p.delayBefore(i));
            assertTrue(p.awaitTurn(i));
        }
        assertTrue(c.sleeps.isEmpty());
    }

    @Test
    public void awaitTurnSleepsTheDelayInSlices() {
        FakeClock c = new FakeClock();
        SafePacer p = new SafePacer(true, fixed(0), c);
        assertTrue(p.awaitTurn(1));
        assertEquals(700, c.slept());
        for (long s : c.sleeps) assertTrue(s <= SafePacer.SLICE_MS);
    }

    @Test
    public void wholeRunOfSixteenItemsTakesExpectedTime() {
        FakeClock c = new FakeClock();
        SafePacer p = new SafePacer(true, fixed(0), c);
        for (int i = 0; i < 16; i++) assertTrue(p.awaitTurn(i));
        // 14 short gaps (items 1-7, 9-15) + 1 long pause (item 8)
        assertEquals(14 * 700 + 3000, c.slept());
    }

    @Test
    public void cancelStopsAWaitPromptly() {
        FakeClock c = new FakeClock();
        SafePacer p = new SafePacer(true, fixed(0.9999999), c);
        c.onSleep = () -> { if (c.now >= 300) p.cancel(); };
        assertFalse(p.awaitTurn(8)); // would be 6 s
        assertTrue(c.slept() <= 300 + SafePacer.SLICE_MS);
        assertTrue(p.isCancelled());
        assertFalse(p.awaitTurn(9));
    }

    @Test
    public void cancelledBeforeStartDoesNotRun() {
        SafePacer p = new SafePacer(false, new Random(), new FakeClock());
        p.cancel();
        assertFalse(p.awaitTurn(0));
    }

    @Test
    public void interruptCancels() {
        SafePacer.Clock interrupting = new SafePacer.Clock() {
            @Override public long nowMs() { return 0; }
            @Override public void sleep(long ms) throws InterruptedException { throw new InterruptedException(); }
        };
        SafePacer p = new SafePacer(true, new Random(3), interrupting);
        assertFalse(p.awaitTurn(1));
        assertTrue(Thread.interrupted()); // flag restored (and cleared here)
    }
}
