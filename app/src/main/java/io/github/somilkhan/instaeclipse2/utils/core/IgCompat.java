package io.github.somilkhan.instaeclipse2.utils.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies the installed Instagram version against the builds this release was checked on.
 * Pure Java so the rules are unit-tested; the Home screen only maps the result to a row.
 *
 * <p>Two kinds of evidence are kept apart on purpose:
 * <ul>
 *   <li>{@link #DEVICE_TESTED}: a maintainer ran this release on a phone with that exact build.</li>
 *   <li>{@link #CHECKED_OFFLINE}: every hook lookup was run against that exact APK with
 *   tools/hook-verify (reports in docs/hook-verify/). Lookups resolve, but nothing was clicked.</li>
 * </ul>
 * Keep both lists in sync with docs/COMPATIBILITY_MATRIX.md.
 */
public final class IgCompat {

    public enum Status {
        /** Exactly a version this release was tested on with a device. */
        TESTED,
        /** Exactly a version the hooks were checked on offline (hook-verify), not on a device. */
        CHECKED_OFFLINE,
        /** Same major as a checked version, different build: most hooks usually still resolve. */
        UNTESTED_BUILD,
        /** Newer major than anything checked. Lookups may miss until the module is updated. */
        NEWER,
        /** Newer major and an alpha (x.0.0.0.y) build from the Play testing channel. */
        NEWER_ALPHA,
        /** Older than the oldest checked major. */
        OLDER,
        /** Version name couldn't be read or parsed. */
        UNKNOWN
    }

    /** Builds this release was run on with a device. None for 0.9.0. */
    public static final String[] DEVICE_TESTED = {};

    /** Builds every hook lookup was checked on with tools/hook-verify. The first one is the main target. */
    public static final String[] CHECKED_OFFLINE = {
            ExportBrand.TARGET_INSTAGRAM, ExportBrand.STABLE_INSTAGRAM,
            "458.0.0.0.57", // alpha the Piko ports were written against; report in docs/hook-verify/
    };

    // Instagram alpha builds look like 458.0.0.0.67: three zero segments after the major.
    private static final Pattern ALPHA = Pattern.compile("^\\d+\\.0\\.0\\.0\\.\\d+$");
    private static final Pattern MAJOR = Pattern.compile("^(\\d+)\\.");

    private IgCompat() {}

    public static Status classify(String versionName) {
        if (versionName == null) return Status.UNKNOWN;
        String v = versionName.trim();
        int major = major(v);
        if (major < 0) return Status.UNKNOWN;
        for (String t : DEVICE_TESTED) if (t.equals(v)) return Status.TESTED;
        for (String t : CHECKED_OFFLINE) if (t.equals(v)) return Status.CHECKED_OFFLINE;
        int min = Integer.MAX_VALUE, max = -1;
        for (String[] list : new String[][]{DEVICE_TESTED, CHECKED_OFFLINE}) {
            for (String t : list) {
                int m = major(t);
                if (m < 0) continue;
                min = Math.min(min, m);
                max = Math.max(max, m);
            }
        }
        if (max < 0) return Status.UNKNOWN;
        if (major > max) return isAlpha(v) ? Status.NEWER_ALPHA : Status.NEWER;
        if (major < min) return Status.OLDER;
        return Status.UNTESTED_BUILD;
    }

    /** True when {@code status} is an exact match with recorded evidence (device or offline). */
    public static boolean isVerified(Status status) {
        return status == Status.TESTED || status == Status.CHECKED_OFFLINE;
    }

    /**
     * The checked build to offer someone on {@code versionName}. Preference order: same major and
     * same channel, same major, same channel, then the main target. "Channel" is alpha
     * (x.0.0.0.y) vs everything else, so an alpha user is never told to leave the testing
     * programme just to get a checked build. Within each rule device-tested builds win over
     * offline-checked ones, then the newest version wins.
     */
    public static String recommendedFor(String versionName) {
        String v = versionName == null ? "" : versionName.trim();
        boolean alpha = isAlpha(v);
        int major = major(v);
        for (int rule = 0; rule < 3; rule++) {
            for (String[] list : new String[][]{DEVICE_TESTED, CHECKED_OFFLINE}) {
                String best = null;
                for (String t : list) {
                    boolean sameMajor = major >= 0 && major(t) == major;
                    boolean sameChannel = isAlpha(t) == alpha;
                    boolean fits = rule == 0 ? sameMajor && sameChannel : rule == 1 ? sameMajor : sameChannel;
                    if (fits && (best == null || compare(t, best) > 0)) best = t;
                }
                if (best != null) return best;
            }
        }
        return ExportBrand.TARGET_INSTAGRAM;
    }

    /** One-line summary for log exports, e.g. "458.0.0.0.67, 457.0.0.55.73 (offline hook check; no device test)". */
    public static String evidenceSummary() {
        StringBuilder sb = new StringBuilder();
        if (DEVICE_TESTED.length > 0) sb.append(String.join(", ", DEVICE_TESTED)).append(" (device-tested)");
        if (CHECKED_OFFLINE.length > 0) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(String.join(", ", CHECKED_OFFLINE)).append(" (offline hook check");
            if (DEVICE_TESTED.length == 0) sb.append("; no device test");
            sb.append(')');
        }
        return sb.toString();
    }

    public static boolean isAlpha(String versionName) {
        return versionName != null && ALPHA.matcher(versionName.trim()).matches();
    }

    /** Numeric, segment-by-segment comparison of two version names; non-numeric segments count as 0. */
    static int compare(String a, String b) {
        String[] x = a.trim().split("\\."), y = b.trim().split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            long p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return p < q ? -1 : 1;
        }
        return 0;
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int major(String v) {
        Matcher m = MAJOR.matcher(v);
        if (!m.find()) return -1;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
