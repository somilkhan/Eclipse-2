package io.github.somilkhan.instaeclipse2.ui.glass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.PathInterpolator;

/** Motion / haptics / accessibility helpers shared by the glass components. */
public final class Motion {
    private Motion() { }

    /** HyperSpring's restrained curves, shared by companion-only entrance and chrome motion. */
    public static final PathInterpolator POWER3_OUT =
            new PathInterpolator(0.215f, 0.61f, 0.355f, 1f);
    public static final PathInterpolator POWER4_OUT =
            new PathInterpolator(0.165f, 0.84f, 0.44f, 1f);
    public static final PathInterpolator BACK_OUT =
            new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);

    /** True when the user turned animations off (Settings › Accessibility › Remove animations). */
    public static boolean reduceMotion() {
        return !ValueAnimator.areAnimatorsEnabled();
    }

    /** TalkBack / Switch Access: never auto-hide chrome, never rely on gestures to reveal it. */
    public static boolean touchExploration(Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
    }

    /** Light "selection changed" tick, the Android equivalent of UISelectionFeedbackGenerator. */
    public static void selectionTick(View v) {
        int c = Build.VERSION.SDK_INT >= 34 ? HapticFeedbackConstants.SEGMENT_TICK
                : HapticFeedbackConstants.CLOCK_TICK;
        v.performHapticFeedback(c);
    }

    public static float dp(Context ctx, float v) {
        return v * ctx.getResources().getDisplayMetrics().density;
    }

    /** Normalised damped spring for short, non-interactive settling motion. */
    public static float spring(float t, float response, float damping) {
        if (t <= 0f) return 0f;
        double z = Math.max(0.05, Math.min(0.999, damping));
        double w0 = 2 * Math.PI / Math.max(0.05, response);
        double wd = w0 * Math.sqrt(1 - z * z);
        double zw = z * w0;
        return (float) (1 - Math.exp(-zw * t)
                * (Math.cos(wd * t) + zw / wd * Math.sin(wd * t)));
    }
}
