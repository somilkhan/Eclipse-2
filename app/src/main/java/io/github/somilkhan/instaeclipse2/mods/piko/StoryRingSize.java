package io.github.somilkhan.instaeclipse2.mods.piko;

/** Story tray avatar size bounds (pure, unit tested). */
public final class StoryRingSize {

    public static final int MIN_DP = 40, MAX_DP = 160, DEFAULT_DP = 100;
    /** Instagram's own min/max avatar size in the tray layout (458.0.0.0.57). */
    public static final float IG_MIN_DP = 66f, IG_MAX_DP = 100f;

    private StoryRingSize() {}

    public static int clamp(int dp) {
        return Math.max(MIN_DP, Math.min(MAX_DP, dp));
    }

    /** Replacement for one of Instagram's bounds, or the input when it isn't one. */
    public static float replace(float igDp, int customDp) {
        if (igDp == IG_MIN_DP || igDp == IG_MAX_DP) return clamp(customDp);
        return igDp;
    }
}
