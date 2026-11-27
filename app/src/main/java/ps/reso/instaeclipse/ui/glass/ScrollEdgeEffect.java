package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.liquidglass.ScrollEdgeBlurView;

/**
 * iOS 26 top scroll-edge effect: content passing under the status bar / compact title is softly
 * blurred and faded instead of hitting a bar with a hairline.
 *
 * <p>API 31+ on a hardware-accelerated window: the library's {@link ScrollEdgeBlurView} (GPU
 * progressive blur) plus our tint gradient for legibility. Otherwise (API 28–30, JVM screenshot
 * tests, high contrast): a {@link BackdropBlurLayout} in edge-fade mode.</p>
 */
public final class ScrollEdgeEffect {
    private final View view;
    @Nullable private final View tint;

    private ScrollEdgeEffect(View view, @Nullable View tint) {
        this.view = view;
        this.tint = tint;
    }

    /** Adds the effect as the top-most child of {@code stage} (which must contain {@code content}). */
    public static ScrollEdgeEffect install(@NonNull FrameLayout stage, @NonNull View content) {
        Context ctx = stage.getContext();
        boolean lib = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !Boolean.getBoolean("instaeclipse.softBlur")
                && !LiquidSupport.highContrast(ctx)
                && !LiquidSupport.isBroken();
        if (lib) {
            try {
                ScrollEdgeBlurView blur = new ScrollEdgeBlurView(ctx);
                blur.setEdge(ScrollEdgeBlurView.Edge.TOP);
                blur.setMaxBlurRadius(Motion.dp(ctx, 18));
                blur.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                stage.addView(blur, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                // Library blur has no tint; add a soft surface-coloured fade on top for legibility.
                BackdropBlurLayout tint = new BackdropBlurLayout(ctx);
                tint.setEdgeFade(true);
                tint.setCornerRadius(0);
                tint.setRimVisible(false);
                tint.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                stage.addView(tint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                return new ScrollEdgeEffect(blur, tint);
            } catch (Throwable t) {
                LiquidSupport.markBroken(t);
            }
        }
        BackdropBlurLayout glass = new BackdropBlurLayout(ctx);
        glass.setEdgeFade(true);
        glass.setCornerRadius(0);
        glass.setRimVisible(false);
        glass.setBlurSource(content);
        glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        stage.addView(glass, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        return new ScrollEdgeEffect(glass, null);
    }

    /** Height of the band (status bar + compact title + fade). */
    public void setHeight(int px) {
        for (View v : new View[]{view, tint}) {
            if (v == null) continue;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != px) {
                lp.height = px;
                v.setLayoutParams(lp);
            }
        }
    }

    /** 0 = at rest over the large title (invisible), 1 = content scrolled underneath. */
    public void setProgress(float p) {
        view.setAlpha(p);
        if (tint != null) tint.setAlpha(p);
        view.setVisibility(p <= 0f ? View.INVISIBLE : View.VISIBLE);
        if (tint != null) tint.setVisibility(view.getVisibility());
    }

    /** Re-binds scroll invalidation to the current screen's scroller (library path only). */
    public void bindScroller(@Nullable View scroller) {
        if (scroller != null && view instanceof ScrollEdgeBlurView) {
            ((ScrollEdgeBlurView) view).bindScrollView(scroller);
        }
    }
}
