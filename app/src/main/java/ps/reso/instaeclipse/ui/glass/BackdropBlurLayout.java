package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import ps.reso.instaeclipse.R;

/**
 * Frosted "liquid glass" chrome (floating nav pill, collapsing top bar).
 *
 * <p>API 31+ on a hardware canvas: the {@link #setBlurSource source} view (the scrolling content
 * underneath) is re-recorded into a {@link RenderNode} every frame, blurred and colour-boosted
 * with a {@link RenderEffect} and drawn clipped to this view's rounded outline — a real backdrop
 * blur that tracks scrolling. API 28–30 (or software canvases): a more opaque tinted fill instead,
 * so text contrast is identical with or without blur. On top: hairline outline + specular rim.
 */
public class BackdropBlurLayout extends FrameLayout {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] myLoc = new int[2];
    private final int[] srcLoc = new int[2];

    private float cornerRadius;
    private float blurRadius;
    private int chromeColor;
    private int fallbackColor;
    private int specularColor;
    private int shaderHeight = -1;
    private boolean rimVisible = true;

    @Nullable private View source;
    private Object node; // RenderNode on API 31+
    private Object effect; // RenderEffect on API 31+
    private boolean drawingBackdrop;

    private final ViewTreeObserver.OnPreDrawListener preDraw = () -> {
        if (source != null && isShown() && getAlpha() > 0f) invalidate();
        return true;
    };

    public BackdropBlurLayout(@NonNull Context context) { this(context, null); }

    public BackdropBlurLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BackdropBlurLayout);
        cornerRadius = a.getDimension(R.styleable.BackdropBlurLayout_ieCornerRadius, -1);
        blurRadius = a.getDimension(R.styleable.BackdropBlurLayout_ieBlurRadius, 28 * d);
        a.recycle();

        chromeColor = ContextCompat.getColor(context, R.color.ie_glass_chrome);
        fallbackColor = ContextCompat.getColor(context, R.color.ie_glass_chrome_fallback);
        specularColor = ContextCompat.getColor(context, R.color.ie_glass_specular);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(Math.max(1f, 0.75f * d));
        outlinePaint.setColor(ContextCompat.getColor(context, R.color.ie_glass_outline));
        specularPaint.setStyle(Paint.Style.STROKE);
        specularPaint.setStrokeWidth(Math.max(1f, d));

        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius());
            }
        });
    }

    /** The view whose pixels this glass refracts (must not be an ancestor of this view). */
    public void setBlurSource(@Nullable View source) {
        if (this.source == source) return;
        detachSource();
        this.source = source;
        if (isAttachedToWindow()) attachSource();
        invalidate();
    }

    /** Edge highlight on/off (e.g. hidden on a fully transparent top bar). */
    public void setRimVisible(boolean visible) {
        if (rimVisible != visible) { rimVisible = visible; invalidate(); }
    }

    /** True when the live backdrop blur path is available on this device. */
    public static boolean blurSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    protected float radius() {
        return cornerRadius < 0 ? getHeight() / 2f : cornerRadius;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachSource();
    }

    @Override
    protected void onDetachedFromWindow() {
        detachSource();
        super.onDetachedFromWindow();
    }

    private void attachSource() {
        if (source != null && blurSupported()) source.getViewTreeObserver().addOnPreDrawListener(preDraw);
    }

    private void detachSource() {
        if (source != null) source.getViewTreeObserver().removeOnPreDrawListener(preDraw);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateOutline();
        shaderHeight = -1;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float r = radius();
        rect.set(0, 0, w, h);
        boolean blurred = false;
        if (blurSupported() && canvas.isHardwareAccelerated() && source != null
                && source.getWidth() > 0 && !drawingBackdrop) {
            blurred = drawBackdrop(canvas, w, h);
        }
        fillPaint.setColor(blurred ? chromeColor : fallbackColor);
        canvas.drawRoundRect(rect, r, r, fillPaint);
        super.onDraw(canvas);
    }

    @Override
    public void onDrawForeground(@NonNull Canvas canvas) {
        super.onDrawForeground(canvas);
        if (!rimVisible) return;
        int w = getWidth(), h = getHeight();
        float r = radius();
        float half = outlinePaint.getStrokeWidth() / 2f;
        rect.set(half, half, w - half, h - half);
        canvas.drawRoundRect(rect, r, r, outlinePaint);
        if (shaderHeight != h) {
            specularPaint.setShader(new LinearGradient(0, 0, 0, Math.max(1f, Math.min(h * 0.6f, r * 1.4f)),
                    specularColor, specularColor & 0x00FFFFFF, Shader.TileMode.CLAMP));
            shaderHeight = h;
        }
        canvas.drawRoundRect(rect, r, r, specularPaint);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private boolean drawBackdrop(Canvas canvas, int w, int h) {
        try {
            if (node == null) {
                node = new RenderNode("ie-glass");
            }
            if (effect == null) {
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.5f);
                effect = RenderEffect.createChainEffect(
                        RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(sat)),
                        RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
            }
            RenderNode rn = (RenderNode) node;
            getLocationInWindow(myLoc);
            source.getLocationInWindow(srcLoc);
            int m = Math.round(blurRadius);
            rn.setPosition(0, 0, w + 2 * m, h + 2 * m);
            rn.setRenderEffect((RenderEffect) effect);
            android.graphics.RecordingCanvas rc = rn.beginRecording();
            rc.translate(-(myLoc[0] - srcLoc[0] - m), -(myLoc[1] - srcLoc[1] - m));
            drawingBackdrop = true;
            try {
                source.draw(rc);
            } finally {
                drawingBackdrop = false;
                rn.endRecording();
            }
            canvas.save();
            canvas.translate(-m, -m);
            canvas.drawRenderNode(rn);
            canvas.restore();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
