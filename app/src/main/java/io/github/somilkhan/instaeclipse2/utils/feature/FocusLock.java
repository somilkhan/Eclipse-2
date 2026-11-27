package io.github.somilkhan.instaeclipse2.utils.feature;

/**
 * Timed Extreme Mode (port of Piko's "Focus Lock"). Extreme Mode already locks the
 * distraction-free switches for good; Focus Lock just adds an end time, kept as epoch millis in
 * {@link FeatureFlags#focusLockUntil}. Pure Java so it can be unit tested.
 *
 * <p>Limit: the end time is wall-clock based, so moving the device clock forward ends it early.
 */
public final class FocusLock {

    private static final long MIN = 60_000L, HOUR = 60 * MIN, DAY = 24 * HOUR;

    /** Piko's duration list: 1/5/10/30 min, 1/2/6/12 h, 1/3 days, 1/2 weeks, 1/3 months. */
    public static final long[] DURATIONS_MS = {
            MIN, 5 * MIN, 10 * MIN, 30 * MIN,
            HOUR, 2 * HOUR, 6 * HOUR, 12 * HOUR,
            DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY, 90 * DAY,
    };

    private FocusLock() {}

    /** Parsed end time, or 0 when unset/invalid. */
    public static long until(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            long v = Long.parseLong(value.trim());
            return v > 0 ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isActive(String value, long now) {
        long u = until(value);
        return u > 0 && now < u;
    }

    /** A lock was set and its time is up: Extreme Mode should be released. */
    public static boolean isExpired(String value, long now) {
        long u = until(value);
        return u > 0 && now >= u;
    }

    public static String endFor(long now, long durationMs) {
        return Long.toString(now + Math.max(0, durationMs));
    }

    /** Compact human label for a duration or remaining time, e.g. "5m", "2h 10m", "3d 4h". */
    public static String format(long ms) {
        if (ms < MIN) return "<1m";
        long d = ms / DAY, h = (ms % DAY) / HOUR, m = (ms % HOUR) / MIN;
        if (d > 0) return h > 0 ? d + "d " + h + "h" : d + "d";
        if (h > 0) return m > 0 ? h + "h " + m + "m" : h + "h";
        return m + "m";
    }
}
