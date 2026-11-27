package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import ps.reso.instaeclipse.R;

/** Inter typeface helper for views built in code. Falls back to the system sans if the font is missing. */
public final class Fonts {
    private Fonts() { }

    /** @param weight 400 regular, 500 medium, 600 semibold, 700 bold. */
    public static Typeface inter(Context ctx, int weight) {
        Typeface base = null;
        try {
            base = ResourcesCompat.getFont(ctx, weight >= 700 ? R.font.inter_display : R.font.inter);
        } catch (Throwable ignored) { }
        if (base == null) base = Typeface.SANS_SERIF;
        return Typeface.create(base, weight, false);
    }
}
