package io.github.somilkhan.instaeclipse2.utils.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies the installed Instagram version against the builds this release was checked on.
 * Pure Java so the rules are unit-tested; the Home screen only maps the result to a row.
 */
public final class IgCompat {

    public enum Status {
        /** Exactly a version the hooks were checked on. */
        TESTED,
        /** Same major as a tested version, different build: most hooks usually still resolve. */
        UNTESTED_BUILD,
        /** Newer major than anything tested. Lookups may miss until the module is updated. */
        NEWER,
        /** Newer major and an alpha (x.0.0.0.y) build from the Play testing channel. */
        NEWER_ALPHA,
        /** Older than the oldest tested major. */
        OLDER,
        /** Version name couldn't be read or parsed. */
        UNKNOWN
    }

    /** Builds checked with tools/hook-verify. The first one is the main target. */
    public static final String[] TESTED = {ExportBrand.TESTED_INSTAGRAM};

    // Instagram alpha builds look like 458.0.0.0.67: three zero segments after the major.
    private static final Pattern ALPHA = Pattern.compile("^\\d+\\.0\\.0\\.0\\.\\d+$");
    private static final Pattern MAJOR = Pattern.compile("^(\\d+)\\.");

    private IgCompat() {}

    public static Status classify(String versionName) {
        if (versionName == null) return Status.UNKNOWN;
        String v = versionName.trim();
        int major = major(v);
        if (major < 0) return Status.UNKNOWN;
        int min = Integer.MAX_VALUE, max = -1;
        for (String t : TESTED) {
            if (t.equals(v)) return Status.TESTED;
            int m = major(t);
            if (m < 0) continue;
            min = Math.min(min, m);
            max = Math.max(max, m);
        }
        if (max < 0) return Status.UNKNOWN;
        if (major > max) return isAlpha(v) ? Status.NEWER_ALPHA : Status.NEWER;
        if (major < min) return Status.OLDER;
        return Status.UNTESTED_BUILD;
    }

    public static boolean isAlpha(String versionName) {
        return versionName != null && ALPHA.matcher(versionName.trim()).matches();
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
