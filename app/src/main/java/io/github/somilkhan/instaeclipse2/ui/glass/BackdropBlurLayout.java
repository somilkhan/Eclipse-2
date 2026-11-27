package io.github.somilkhan.instaeclipse2.ui.glass;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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

import io.github.somilkhan.instaeclipse2.R;

/**
 * In-house "liquid glass" material, used wherever the Liquid-Glass-Android lens is not available
 * (API 28–32, x86 devices without its native helper, battery saver, high contrast, any runtime
 * failure of the library — see {@link LiquidSupport}) and for the JVM screenshot tests.
 *
 * <p>Backdrop: API 31+ on a hardware canvas re-records the {@link #setBlurSource source} into a
 * {@link RenderNode} with a blur + saturation {@link RenderEffect}; otherwise the source is drawn
 * into a small bitmap and box-blurred on the CPU. On top of the blurred backdrop:</p>
 * <ul>
 *   <li><b>rim refraction</b> ({@code ieRefraction}): two bands along the edge redraw the backdrop
 *       minified towards the centre, so content just outside the shape appears bent into the rim,
 *       with a slight red/cyan split on the outer band (the lens' chromatic dispersion);</li>
 *   <li>a translucent tint (more opaque without blur), a diagonal specular rim (bright top-left,
 *       softer bottom-right reflection) and a hairline;</li>
 *   <li><b>edge fade</b> ({@code ieEdgeFade}): the iOS 26 scroll-edge effect — blur and tint fade
 *       out towards the bottom, no rim, no hairline.</li>
 * </ul>
 * With "high contrast text" on (Android's nearest to Reduce Transparency) the glass is opaque.
 * The glass only redraws when its source is actually dirty, so a static screen costs nothing.
 */
public class BackdropBlurLayout extends FrameLayout {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint redPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint cyanPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint maskPaint = new Paint();
    private final Paint tintGradPaint = new Paint();
    private final RectF rect = new RectF();
    private final Path outerPath = new Path();
    private final Path innerPath = new Path();
    private final Path ringPath = new Path();
    private final int[] myLoc = new int[2];
    private final int[] srcLoc = new int[2];
    private final float density;

    private float cornerRadius;
    private float blurRadius;
    private int chromeColor;
    private int fallbackColor;
    private int rimColor;
    private int shaderW = -1, shaderH = -1;
    private boolean rimVisible = true;
    private boolean refraction;
    private boolean edgeFade;
    private boolean glassDrawingEnabled = true;
    private final boolean nightMode;

    @Nullable private View source;
    private Object node; // RenderNode on API 31+
    private Object effect; // RenderEffect on API 31+
    private boolean drawingBackdrop;
    private boolean hwBroken;

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

    /** Which backdrop was prepared for this frame. */
    private static final int BD_NONE = 0, BD_NODE = 1, BD_SOFT = 2;
    private int backdropKind;
    private int margin;

    private final ViewTreeObserver.OnPreDrawListener preDraw = () -> {
        if (source != null && source.isDirty() && isShown() && getAlpha() > 0f) invalidate();
        return true;
    };

    public BackdropBlurLayout(@NonNull Context context) { this(context, null); }

    public BackdropBlurLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BackdropBlurLayout);
        cornerRadius = a.getDimension(R.styleable.BackdropBlurLayout_ieCornerRadius, -1);
        blurRadius = a.getDimension(R.styleable.BackdropBlurLayout_ieBlurRadius, 28 * density);
        refraction = a.getBoolean(R.styleable.BackdropBlurLayout_ieRefraction, false);
        edgeFade = a.getBoolean(R.styleable.BackdropBlurLayout_ieEdgeFade, false);
        a.recycle();

        nightMode = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        chromeColor = ContextCompat.getColor(context, R.color.ie_glass_chrome);
        fallbackColor = ContextCompat.getColor(context, R.color.ie_glass_chrome_fallback);
        rimColor = ContextCompat.getColor(context, R.color.ie_glass_rim);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(Math.max(1f, 0.6f * density));
        outlinePaint.setColor(ContextCompat.getColor(context, R.color.ie_glass_outline));
        specularPaint.setStyle(Paint.Style.STROKE);
        specularPaint.setStrokeWidth(Math.max(1f, 1.1f * density));
        redPaint.setColorFilter(new LightingColorFilter(0xFFFF0000, 0));
        cyanPaint.setColorFilter(new LightingColorFilter(0xFF00FFFF, 0));
        cyanPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                glassOutline(outline);
            }
        });
    }

    /** Outline used for clipping and the drop shadow; subclasses may shrink it (minimised pill). */
    protected void glassOutline(Outline outline) {
        outline.setRoundRect(0, 0, getWidth(), getHeight(), radius());
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

    /** Corner radius in px; negative = capsule. */
    public void setCornerRadius(float px) {
        cornerRadius = px;
        invalidateOutline();
        invalidate();
    }

    public void setRefraction(boolean on) {
        if (refraction != on) { refraction = on; invalidate(); }
    }

    public void setEdgeFade(boolean on) {
        if (edgeFade != on) { edgeFade = on; shaderH = -1; invalidate(); }
    }

    /**
     * Off when a library lens draws the glass instead (the layout then only hosts children; the
     * fallback comes back automatically if the library fails).
     */
    public void setGlassDrawingEnabled(boolean on) {
        if (glassDrawingEnabled != on) { glassDrawingEnabled = on; invalidate(); }
    }

    public boolean isGlassDrawingEnabled() { return glassDrawingEnabled; }

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
        shaderW = shaderH = -1;
    }

    /** Glass rectangle in view coordinates (full bounds by default). */
    protected void glassRect(RectF out) {
        out.set(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (glassDrawingEnabled) drawGlass(canvas);
        super.onDraw(canvas);
    }

    private void drawGlass(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        boolean opaque = LiquidSupport.highContrast(getContext());
        backdropKind = BD_NONE;
        if (!opaque && source != null && source.getWidth() > 0 && !drawingBackdrop) {
            if (!FORCE_SOFT && !hwBroken && blurSupported() && canvas.isHardwareAccelerated()) {
                if (prepareNode(w, h)) backdropKind = BD_NODE;
            }
            if (backdropKind == BD_NONE && !softBroken && prepareSoft(w, h)) backdropKind = BD_SOFT;
        }
        glassRect(rect);
        float r = Math.min(radius(), rect.height() / 2f);

        if (edgeFade) {
            drawEdgeFade(canvas, w, h, opaque);
            return;
        }
        if (backdropKind != BD_NONE) {
            canvas.save();
            outerPath.reset();
            outerPath.addRoundRect(rect, r, r, Path.Direction.CW);
            canvas.clipPath(outerPath);
            paintBackdrop(canvas, 1f, 1f, null);
            if (refraction) drawRefractionRim(canvas, r);
            canvas.restore();
        }
        fillPaint.setColor(backdropKind != BD_NONE ? chromeColor : fallbackColor);
        canvas.drawRoundRect(rect, r, r, fillPaint);
    }

    /**
     * Rim refraction approximation: inside a band along the edge, redraw the backdrop scaled
     * down about the centre (each edge samples ~k px further out), like light bent by a bevel.
     */
    private void drawRefractionRim(Canvas canvas, float r) {
        float minSide = Math.min(rect.width(), rect.height());
        float bevel = Math.min(14 * density, minSide * 0.30f);
        if (bevel < 3 * density) return;
        // Inner band: gentle bend.
        ring(r, bevel);
        canvas.save();
        canvas.clipPath(ringPath);
        float k = bevel * 0.9f;
        paintBackdrop(canvas, 1f - 2f * k / rect.width(), 1f - 2f * k / rect.height(), null);
        canvas.restore();
        // Outer band: strongest bend + chromatic split (red sampled a bit further out than cyan).
        float outer = bevel * 0.45f;
        ring(r, outer);
        canvas.save();
        canvas.clipPath(ringPath);
        float k2 = bevel * 1.7f;
        paintBackdrop(canvas, 1f - 2f * (k2 * 1.08f) / rect.width(), 1f - 2f * (k2 * 1.08f) / rect.height(), redPaint);
        paintBackdrop(canvas, 1f - 2f * (k2 * 0.94f) / rect.width(), 1f - 2f * (k2 * 0.94f) / rect.height(), cyanPaint);
        canvas.restore();
    }

    private void ring(float r, float width) {
        outerPath.reset();
        outerPath.addRoundRect(rect, r, r, Path.Direction.CW);
        innerPath.reset();
        RectF in = new RectF(rect);
        in.inset(width, width);
        float ir = Math.max(0f, r - width);
        innerPath.addRoundRect(in, ir, ir, Path.Direction.CW);
        ringPath.reset();
        ringPath.op(outerPath, innerPath, Path.Op.DIFFERENCE);
    }

    private void drawEdgeFade(Canvas canvas, int w, int h, boolean opaque) {
        int save = canvas.saveLayer(0, 0, w, h, null);
        if (backdropKind != BD_NONE) paintBackdrop(canvas, 1f, 1f, null);
        if (shaderH != h || shaderW != w) {
            int base = opaque || (backdropKind == BD_NONE && source != null) ? fallbackColor : chromeColor;
            tintGradPaint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{base, base, base & 0x00FFFFFF},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            maskPaint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{Color.BLACK, Color.BLACK, 0xB0000000, Color.TRANSPARENT},
                    new float[]{0f, 0.5f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            shaderW = w;
            shaderH = h;
        }
        canvas.drawRect(0, 0, w, h, tintGradPaint);
        canvas.drawRect(0, 0, w, h, maskPaint);
        canvas.restoreToCount(save);
    }

    /** Draws the prepared backdrop scaled by (sx, sy) about the glass centre. */
    private void paintBackdrop(Canvas canvas, float sx, float sy, @Nullable Paint layerPaint) {
        canvas.save();
        float cx = rect.centerX(), cy = rect.centerY();
        canvas.scale(sx, sy, cx, cy);
        if (backdropKind == BD_SOFT && softBitmap != null) {
            canvas.drawBitmap(softBitmap, softSrc, softDst, layerPaint != null ? layerPaint : softPaint);
        } else if (backdropKind == BD_NODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int s = layerPaint != null ? canvas.saveLayer(null, layerPaint) : -1;
            canvas.translate(-margin, -margin);
            canvas.drawRenderNode((RenderNode) node);
            if (s >= 0) canvas.restoreToCount(s);
        }
        canvas.restore();
    }

    @Override
    public void onDrawForeground(@NonNull Canvas canvas) {
        super.onDrawForeground(canvas);
        if (!glassDrawingEnabled || !rimVisible || edgeFade) return;
        glassRect(rect);
        float r = Math.min(radius(), rect.height() / 2f);
        float half = outlinePaint.getStrokeWidth() / 2f;
        rect.inset(half, half);
        boolean opaque = LiquidSupport.highContrast(getContext());
        outlinePaint.setAlpha(opaque ? 255 : Color.alpha(ContextCompat.getColor(getContext(), R.color.ie_glass_outline)));
        canvas.drawRoundRect(rect, r, r, outlinePaint);
        if (opaque) return;
        int w = Math.round(rect.width()), h = Math.round(rect.height());
        if (shaderW != w || shaderH != h) {
            int bright = rimColor;
            int faint = rimColor & 0x00FFFFFF | (Math.round(Color.alpha(rimColor) * 0.12f) << 24);
            int mid = rimColor & 0x00FFFFFF | (Math.round(Color.alpha(rimColor) * 0.55f) << 24);
            specularPaint.setShader(new LinearGradient(rect.left, rect.top, rect.left + Math.min(w, h * 4f), rect.bottom,
                    new int[]{bright, faint, faint, mid}, new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.MIRROR));
            shaderW = w;
            shaderH = h;
        }
        canvas.drawRoundRect(rect, r - half, r - half, specularPaint);
    }

    private boolean prepareSoft(int w, int h) {
        try {
            int m = Math.round(blurRadius) + Math.round(24 * density);
            int bw = Math.max(1, (int) Math.ceil((w + 2 * m) * SOFT_SCALE));
            int bh = Math.max(1, (int) Math.ceil((h + 2 * m) * SOFT_SCALE));
            if (softBitmap == null || softBitmap.getWidth() != bw || softBitmap.getHeight() != bh) {
                softBitmap = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888);
                softCanvas = new Canvas(softBitmap);
                softPixels = new int[bw * bh];
                softScratch = new int[bw * bh];
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.6f);
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
                drawWindowBackground(softCanvas, m);
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
            softDst.set(-m, -m, -m + bw / SOFT_SCALE, -m + bh / SOFT_SCALE);
            margin = m;
            return true;
        } catch (Throwable t) {
            softBroken = true; // e.g. hardware bitmaps can't be drawn in software: tinted fallback
            return false;
        }
    }

    /**
     * The source may be transparent over the window background: paint that first so the
     * backdrop is opaque and fully hides the sharp content underneath the glass.
     */
    private void drawWindowBackground(Canvas c, int m) {
        View root = getRootView();
        android.graphics.drawable.Drawable bg = root.getBackground();
        if (bg == null) return;
        // Explicit bounds, extended past the window edges so the blur never samples "nothing"
        // (which would fade the glass to transparent at the screen edges).
        int mm = m * 2;
        android.graphics.Rect saved = bg.copyBounds();
        bg.setBounds(-mm, -mm, root.getWidth() + mm, root.getHeight() + mm);
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
    private boolean prepareNode(int w, int h) {
        try {
            if (node == null) {
                node = new RenderNode("ie-glass");
            }
            if (effect == null) {
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.6f);
                effect = RenderEffect.createChainEffect(
                        RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(sat)),
                        RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
            }
            RenderNode rn = (RenderNode) node;
            getLocationInWindow(myLoc);
            source.getLocationInWindow(srcLoc);
            int m = Math.round(blurRadius) + Math.round(24 * density);
            rn.setPosition(0, 0, w + 2 * m, h + 2 * m);
            rn.setRenderEffect((RenderEffect) effect);
            android.graphics.RecordingCanvas rc = rn.beginRecording();
            rc.translate(-(myLoc[0] - srcLoc[0] - m), -(myLoc[1] - srcLoc[1] - m));
            drawingBackdrop = true;
            try {
                drawWindowBackground(rc, m);
                source.draw(rc);
            } finally {
                drawingBackdrop = false;
                rn.endRecording();
            }
            margin = m;
            return true;
        } catch (Throwable t) {
            hwBroken = true;
            return false;
        }
    }
}
