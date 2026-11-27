package io.github.somilkhan.instaeclipse2.utils.pacing;

import java.util.Random;

/**
 * Safe pacing for bulk actions (bulk unsend, "Download all", comment search "load all").
 *
 * <p>Between two items it waits a random 0.7–1.6 s, and before every {@value #LONG_PAUSE_EVERY}th
 * item a longer random 3–6 s pause, so bulk actions run at a human-like rate instead of as fast as
 * the device can fire requests. It only slows the module's own actions down; it does not hide or
 * alter anything about the device or account.
 *
 * <p>Pure Java: the {@link Random} and the {@link Clock} are injected so the timing is unit tested.
 * Waits are sliced so {@link #cancel()} takes effect within {@value #SLICE_MS} ms.
 */
public final class SafePacer {

    public static final long MIN_DELAY_MS = 700;
    public static final long MAX_DELAY_MS = 1600;
    public static final long LONG_MIN_MS = 3000;
    public static final long LONG_MAX_MS = 6000;
    public static final int LONG_PAUSE_EVERY = 8;
    static final long SLICE_MS = 100;

    /** Time source + sleep, injectable for tests. */
    public interface Clock {
        long nowMs();

        void sleep(long ms) throws InterruptedException;

        Clock SYSTEM = new Clock() {
            @Override public long nowMs() { return System.nanoTime() / 1_000_000L; }
            @Override public void sleep(long ms) throws InterruptedException { Thread.sleep(ms); }
        };
    }

    private final boolean enabled;
    private final Random random;
    private final Clock clock;
    private volatile boolean cancelled;

    public SafePacer(boolean enabled, Random random, Clock clock) {
        this.enabled = enabled;
        this.random = random;
        this.clock = clock;
    }

    /** Production pacer; {@code enabled} is normally {@code FeatureFlags.safePacing}. */
    public static SafePacer create(boolean enabled) {
        return new SafePacer(enabled, new Random(), Clock.SYSTEM);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Delay to wait before item {@code index} (0-based). The first item starts immediately; items
     * 8, 16, 24… get the long pause; every other item gets the short random delay. 0 when disabled.
     */
    public long delayBefore(int index) {
        if (!enabled || index <= 0) return 0;
        if (index % LONG_PAUSE_EVERY == 0) return between(LONG_MIN_MS, LONG_MAX_MS);
        return between(MIN_DELAY_MS, MAX_DELAY_MS);
    }

    /** Uniform in [min, max] inclusive. */
    private long between(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min + 1));
    }

    /**
     * Blocks (background threads only) for {@link #delayBefore(int)}. Returns false if the run was
     * cancelled before or during the wait, true if the caller should go on with item {@code index}.
     */
    public boolean awaitTurn(int index) {
        if (cancelled) return false;
        long wait = delayBefore(index);
        if (wait <= 0) return !cancelled;
        long end = clock.nowMs() + wait;
        try {
            for (long left = wait; left > 0 && !cancelled; left = end - clock.nowMs()) {
                clock.sleep(Math.min(SLICE_MS, left));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelled = true;
        }
        return !cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
