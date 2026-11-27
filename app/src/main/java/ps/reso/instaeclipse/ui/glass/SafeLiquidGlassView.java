package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.liquidglass.LiquidGlassView;

/**
 * {@link LiquidGlassView} that can never take the app down: any {@link Throwable} thrown while
 * measuring, sizing or drawing (missing native code, shader compile failure on an odd GPU driver,
 * ...) hides the view, marks the library broken and tells the host so it can switch back to the
 * in-house {@link BackdropBlurLayout} glass.
 */
public class SafeLiquidGlassView extends LiquidGlassView {

    public interface OnBrokenListener { void onGlassBroken(); }

    @Nullable private OnBrokenListener brokenListener;
    private boolean failed;

    public SafeLiquidGlassView(@NonNull Context context) {
        super(context);
    }

    public void setOnBrokenListener(@Nullable OnBrokenListener l) { brokenListener = l; }

    public boolean hasFailed() { return failed; }

    private void fail(Throwable t) {
        if (failed) return;
        failed = true;
        LiquidSupport.markBroken(t);
        post(() -> {
            setVisibility(View.GONE);
            if (brokenListener != null) brokenListener.onGlassBroken();
        });
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (failed) return;
        try {
            super.draw(canvas);
        } catch (Throwable t) {
            fail(t);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (failed) return;
        try {
            super.onDraw(canvas);
        } catch (Throwable t) {
            fail(t);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        try {
            super.onSizeChanged(w, h, oldw, oldh);
        } catch (Throwable t) {
            fail(t);
        }
    }
}
