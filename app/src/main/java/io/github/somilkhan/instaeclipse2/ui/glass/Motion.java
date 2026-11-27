package io.github.somilkhan.instaeclipse2.ui.glass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

/** Motion / haptics / accessibility helpers shared by the glass components. */
public final class Motion {
    private Motion() { }

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
}
