package io.github.somilkhan.instaeclipse2.utils.ghost;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

/**
 * Short "let seen through" window for the manual Mark as read buttons (DM and channel).
 *
 * <p>Those buttons used to set {@code FeatureFlags.isGhostSeen = false} and later force it back
 * to {@code true}. That overwrote the user's setting. A button still on screen after Ghost seen
 * was switched off (quick toggle) silently switched it back on, a toggle during the window was
 * lost, and a callback that never ran left seen-blocking off. The setting is now never written
 * here: callers open a window with {@link #begin()} and close it with {@link Window#close()}, and
 * the seen hooks ask {@link #blocksSeen()}. Every window also expires after {@link #MAX_MS} so
 * a lost close cannot keep seen-blocking off.
 */
public final class GhostSeenBypass {

    /** Upper bound for one window; the buttons close theirs after 300–400 ms. */
    public static final long MAX_MS = 2_000;

    static volatile LongSupplier clock = () -> System.nanoTime() / 1_000_000L;

    private static final AtomicInteger OPEN = new AtomicInteger();
    private static volatile long deadline;

    private GhostSeenBypass() {}

    /** One open window. {@link #close()} is idempotent. */
    public static final class Window implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean();

        private Window() {}

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) OPEN.decrementAndGet();
        }
    }

    public static Window begin() {
        long until = clock.getAsLong() + MAX_MS;
        synchronized (GhostSeenBypass.class) {
            if (OPEN.getAndIncrement() == 0 || until > deadline) deadline = until;
        }
        return new Window();
    }

    /** True while at least one window is open and not expired. */
    public static boolean active() {
        return OPEN.get() > 0 && clock.getAsLong() < deadline;
    }

    /** What the seen hooks check instead of {@code FeatureFlags.isGhostSeen}. */
    public static boolean blocksSeen() {
        return FeatureFlags.isGhostSeen && !active();
    }

    /** Test hook: drop all windows. */
    static void resetForTest() {
        OPEN.set(0);
        deadline = 0;
    }
}
