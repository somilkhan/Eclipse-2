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
 * blur that tracks scrolling. API 28–30 (and software canvases): the source is drawn into a small
 * down-scaled bitmap and box-blurred on the CPU (cheap because the region is tiny). If either path
 * fails (e.g. hardware bitmaps in a software draw) a more opaque tinted fill is used, so text
 * contrast never depends on the blur. On top: hairline outline + specular rim.
 *
 * <p>The glass only redraws when its source is actually dirty, so a static screen costs nothing.
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

    // Software blur (API 28-30 / forced for JVM screenshot tests, where RenderEffect is a no-op).
    private static final float SOFT_SCALE = 1f / 6f;
    private static final boolean FORCE_SOFT = Boolean.getBoolean("instaeclipse.softBlur");
    @Nullable private android.graphics.Bitmap softBitmap;
    @Nullable private Canvas softCanvas;
    private int[] softPixels = new int[0];
    private int[] softScratch = new int[0];
    private final Paint softPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Rect softSrc = new android.graphics.Rect();
    private final RectF softDst = new RectF();
    private boolean softBroken;

    private final ViewTreeObserver.OnPreDrawListener preDraw = () -> {
        if (source != null && source.isDirty() && isShown() && getAlpha() > 0f) invalidate();
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
        if (source != null) source.getViewTreeObserver().addOnPreDrawListener(preDraw);
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
        if (source != null && source.getWidth() > 0 && w > 0 && h > 0 && !drawingBackdrop) {
            if (!FORCE_SOFT && blurSupported() && canvas.isHardwareAccelerated()) {
                blurred = drawBackdrop(canvas, w, h);
            } else if (!softBroken) {
                blurred = drawSoftBackdrop(canvas, w, h);
            }
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

    private boolean drawSoftBackdrop(Canvas canvas, int w, int h) {
        try {
            int m = Math.round(blurRadius);
            int bw = Math.max(1, (int) Math.ceil((w + 2 * m) * SOFT_SCALE));
            int bh = Math.max(1, (int) Math.ceil((h + 2 * m) * SOFT_SCALE));
            if (softBitmap == null || softBitmap.getWidth() != bw || softBitmap.getHeight() != bh) {
                softBitmap = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888);
                softCanvas = new Canvas(softBitmap);
                softPixels = new int[bw * bh];
                softScratch = new int[bw * bh];
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.5f);
                softPaint.setColorFilter(new ColorMatrixColorFilter(sat));
            }
            getLocationInWindow(myLoc);
            source.getLocationInWindow(srcLoc);
            softBitmap.eraseColor(0);
            softCanvas.save();
            softCanvas.scale(SOFT_SCALE, SOFT_SCALE);
            softCanvas.translate(-(myLoc[0] - srcLoc[0] - m), -(myLoc[1] - srcLoc[1] - m));
            drawingBackdrop = true;
            try {
                drawWindowBackground(softCanvas);
                source.draw(softCanvas);
            } finally {
                drawingBackdrop = false;
                softCanvas.restore();
            }
            int r = Math.max(1, Math.round(blurRadius * SOFT_SCALE / 2f));
            softBitmap.getPixels(softPixels, 0, bw, 0, 0, bw, bh);
            for (int i = 0; i < 3; i++) { // 3 box passes ≈ gaussian
                boxBlur(softPixels, softScratch, bw, bh, r, true);
                boxBlur(softScratch, softPixels, bw, bh, r, false);
            }
            softBitmap.setPixels(softPixels, 0, bw, 0, 0, bw, bh);
            softSrc.set(0, 0, bw, bh);
            softDst.set(-m, -m, w + m, h + m);
            canvas.drawBitmap(softBitmap, softSrc, softDst, softPaint);
            return true;
        } catch (Throwable t) {
            softBroken = true; // e.g. hardware bitmaps can't be drawn in software: tinted fallback
            return false;
        }
    }

    /**
     * The source is transparent over the window's mesh background: paint that first so the
     * backdrop is opaque and fully hides the sharp content underneath the glass.
     */
    private void drawWindowBackground(Canvas c) {
        View root = getRootView();
        android.graphics.drawable.Drawable bg = root.getBackground();
        if (bg == null) return;
        // Explicit bounds, extended past the window edges so the blur never samples "nothing"
        // (which would fade the glass to transparent at the screen edges).
        int m = Math.round(blurRadius) * 2;
        android.graphics.Rect saved = bg.copyBounds();
        bg.setBounds(-m, -m, root.getWidth() + m, root.getHeight() + m);
        c.save();
        c.translate(-srcLoc[0], -srcLoc[1]);
        bg.draw(c);
        c.restore();
        bg.setBounds(saved);
    }

    /** One horizontal or vertical running-sum box blur pass (premultiplied ARGB, edge clamped). */
    private static void boxBlur(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h, lines = horizontal ? h : w;
        int div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int k = -r; k <= r; k++) {
                int c = in[idx(line, clamp(k, len), w, horizontal)];
                sa += c >>> 24; sr += (c >> 16) & 0xFF; sg += (c >> 8) & 0xFF; sb += c & 0xFF;
            }
            for (int i = 0; i < len; i++) {
                out[idx(line, i, w, horizontal)] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                int cOut = in[idx(line, clamp(i - r, len), w, horizontal)];
                int cIn = in[idx(line, clamp(i + r + 1, len), w, horizontal)];
                sa += (cIn >>> 24) - (cOut >>> 24);
                sr += ((cIn >> 16) & 0xFF) - ((cOut >> 16) & 0xFF);
                sg += ((cIn >> 8) & 0xFF) - ((cOut >> 8) & 0xFF);
                sb += (cIn & 0xFF) - (cOut & 0xFF);
            }
        }
    }

    private static int clamp(int i, int len) { return i < 0 ? 0 : (i >= len ? len - 1 : i); }

    private static int idx(int line, int i, int w, boolean horizontal) {
        return horizontal ? line * w + i : i * w + line;
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
                drawWindowBackground(rc);
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
