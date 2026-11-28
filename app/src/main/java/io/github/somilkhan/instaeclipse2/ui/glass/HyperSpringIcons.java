package io.github.somilkhan.instaeclipse2.ui.glass;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.AnimationUtils;

/**
 * Direct companion-safe adaptation of HyperSpring's Icons drawable (Apache-2.0).
 * Only HOME, SLIDERS, LIST, and INFO are retained; launcher/System UI glyphs are omitted.
 * Source: https://github.com/olilio3/HyperSpring, app/src/main/java/com/icehome/up/Icons.java
 */
final class HyperSpringIcons extends Drawable {
    static final int HOME = 0;
    static final int SLIDERS = 1;
    static final int LIST = 2;
    static final int INFO = 3;

    private static final float GESTURE_MS = 1400f;
    private static final float REST = 1e6f;
    private static final float[] LIST_ROWS = {9f, 12.5f, 16f};
    private static final float[] LIST_ENDS = {16f, 16f, 12.5f};

    private final int kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path part = new Path();
    private final Matrix gestureMatrix = new Matrix();
    private final Matrix partMatrix = new Matrix();
    private final RectF rect = new RectF();
    private long playStart = -1L;
    private final Runnable nextFrame = this::invalidateSelf;

    HyperSpringIcons(int kind, int color) {
        this.kind = kind;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        dot.setStyle(Paint.Style.FILL);
    }

    void setColor(int color) {
        if (paint.getColor() != color) {
            paint.setColor(color);
            invalidateSelf();
        }
    }

    /** Replays the source gesture once; Drawable scheduling is serviced by GlassNavPill's one frame loop. */
    void play() {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finish();
            return;
        }
        playStart = AnimationUtils.currentAnimationTimeMillis();
        invalidateSelf();
        scheduleSelf(nextFrame, android.os.SystemClock.uptimeMillis() + 8L);
    }

    boolean isAnimating(long now) {
        if (playStart < 0L) return false;
        float scale = ValueAnimator.getDurationScale();
        long elapsed = now - playStart;
        if (elapsed >= GESTURE_MS * (scale > 0f ? scale : 1f)) {
            playStart = -1L;
            unscheduleSelf(nextFrame);
            return false;
        }
        return true;
    }

    void finish() {
        playStart = -1L;
        unscheduleSelf(nextFrame);
        invalidateSelf();
    }

    private float gestureTime() {
        if (playStart < 0L) return -1f;
        float scale = ValueAnimator.getDurationScale();
        float t = (AnimationUtils.currentAnimationTimeMillis() - playStart) / (scale > 0f ? scale : 1f);
        if (t >= GESTURE_MS) {
            playStart = -1L;
            return -1f;
        }
        return t;
    }

    private static float seg(float t, float a, float b) {
        return Math.max(0f, Math.min(1f, (t - a) / (b - a)));
    }

    private static float hump(float t, float a, float b) {
        return (float) Math.sin(Math.PI * seg(t, a, b));
    }

    private static float bump(float t, float delay) {
        if (t <= delay) return 0f;
        float u = t - delay;
        return (float) (Math.exp(-u / 170f) * Math.sin(2 * Math.PI * u / 380f));
    }

    private static float spring(float t, float response, float damping) {
        if (t <= 0f) return 0f;
        double z = Math.max(0.05, Math.min(0.999, damping));
        double w0 = 2 * Math.PI / Math.max(0.05, response);
        double wd = w0 * Math.sqrt(1 - z * z);
        double zw = z * w0;
        return (float) (1 - Math.exp(-zw * t)
                * (Math.cos(wd * t) + zw / wd * Math.sin(wd * t)));
    }

    private static float springPart(float t, float delay, float response, float damping) {
        return t <= delay ? 0f : spring((t - delay) / 1000f, response, damping);
    }

    private static float rubber(float t) {
        if (t <= 0f) return 0f;
        return (float) (Math.exp(-t / 200f) * Math.sin(2 * Math.PI * t / 340f));
    }

    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        // HyperSpring Motion.EASE = cubic Bezier (0.2, 0, 0, 1).
        float lo = 0f, hi = 1f, t = x;
        for (int i = 0; i < 8; i++) {
            float bx = bezier(t, 0.2f, 0f);
            if (bx < x) lo = t; else hi = t;
            t = (lo + hi) * 0.5f;
        }
        return bezier(t, 0f, 1f);
    }

    private static float bezier(float t, float p1, float p2) {
        float u = 1f - t;
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t;
    }

    private static float wave(float t, float delay, float ms) {
        float u = t - delay;
        if (u <= 0f) return 0f;
        if (u < ms) return (float) Math.sin(Math.PI * ease(u / ms));
        return -0.25f * rubber(u - ms);
    }

    private void gesture(Matrix out, float t) {
        switch (kind) {
            case HOME: {
                float crouch = hump(t, 0f, 150f);
                out.preScale(1f + 0.12f * crouch, 1f - 0.16f * crouch, 12f, 20f);
                float jump = seg(t, 110f, 430f);
                out.preTranslate(0f, -6.5f * 4f * jump * (1f - jump));
                float air = hump(t, 110f, 430f);
                out.preScale(1f - 0.06f * air, 1f + 0.1f * air, 12f, 20f);
                float squash = rubber(t - 430f);
                out.preScale(1f + 0.22f * squash, 1f - 0.24f * squash, 12f, 20f);
                break;
            }
            case INFO: {
                float f = ease(t / 520f);
                float sx = (float) Math.cos(2 * Math.PI * f);
                out.preScale(Math.abs(sx) < 0.08f ? 0.08f : sx, 1f, 12f, 12f);
                float r = rubber(t);
                out.preScale(1f + 0.12f * r, 1f + 0.12f * r, 12f, 12f);
                break;
            }
            case SLIDERS:
                break; // The two controls move independently in draw().
            case LIST: {
                float k = t < 100f ? 1f - 0.25f * ease(t / 100f)
                        : 0.75f + 0.25f * springPart(t, 100f, 0.42f, 0.45f);
                out.preScale(k, k, 4f, 20.5f);
                break;
            }
            default:
                break;
        }
    }

    private void effects(Canvas canvas, float t) {
        if (kind == HOME) splash(canvas, t, 430f, 700f, 12f, 20.5f, 5f, 4f, 1.5f, 1.1f);
    }

    private void splash(Canvas canvas, float t, float start, float end, float cx, float cy,
                        float gap, float out, float up, float size) {
        if (t <= start || t >= end) return;
        float f = seg(t, start, end);
        float x = gap + out * (1f - (float) Math.pow(1f - f, 3f));
        float y = -up * 4f * f * (1f - f);
        dot.setColor(paint.getColor());
        dot.setAlpha(Math.round(paint.getAlpha() * (1f - f * f)));
        float s = size * (1f - 0.6f * f);
        canvas.drawCircle(cx - x, cy + y, s, dot);
        canvas.drawCircle(cx + x, cy + y, s, dot);
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        float t = gestureTime();
        float time = t >= 0f ? t : REST;
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        float s = Math.min(bounds.width(), bounds.height()) / 24f;
        canvas.scale(s, s);
        gestureMatrix.reset();
        if (t >= 0f) gesture(gestureMatrix, t);
        path.reset();
        switch (kind) {
            case HOME:
                path.moveTo(4, 10.5f);
                path.lineTo(12, 4);
                path.lineTo(20, 10.5f);
                path.lineTo(20, 19);
                path.quadTo(20, 20, 19, 20);
                path.lineTo(15, 20);
                path.lineTo(15, 14.5f);
                path.lineTo(9, 14.5f);
                path.lineTo(9, 20);
                path.lineTo(5, 20);
                path.quadTo(4, 20, 4, 19);
                path.close();
                break;
            case SLIDERS:
                path.moveTo(4, 7);
                path.lineTo(20, 7);
                path.moveTo(4, 17);
                path.lineTo(20, 17);
                path.addCircle(9 + 6f * wave(time, 0f, 600f), 7, 2.2f, Path.Direction.CW);
                path.addCircle(15 - 6f * wave(time, 90f, 600f), 17, 2.2f, Path.Direction.CW);
                break;
            case LIST: {
                rect.set(4, 3.5f, 20, 20.5f);
                path.addRoundRect(rect, 2.5f, 2.5f, Path.Direction.CW);
                for (int i = 0; i < 3; i++) {
                    float k = springPart(time, 140f + 70f * i, 0.32f, 0.75f);
                    if (k <= 0.01f) continue;
                    path.moveTo(8, LIST_ROWS[i]);
                    path.lineTo(8 + (LIST_ENDS[i] - 8) * k, LIST_ROWS[i]);
                }
                break;
            }
            case INFO:
                path.addCircle(12, 12, 8.5f, Path.Direction.CW);
                path.moveTo(12, 11);
                path.lineTo(12, 16.5f);
                path.addCircle(12, 7.8f - 3.5f * hump(time, 420f, 660f), 0.4f, Path.Direction.CW);
                break;
            default:
                break;
        }
        path.transform(gestureMatrix);
        canvas.drawPath(path, paint);
        if (t >= 0f) effects(canvas, t);
        canvas.restore();
        if (playStart >= 0L) scheduleSelf(nextFrame, android.os.SystemClock.uptimeMillis() + 8L);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); dot.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return -1; }
    @Override public int getIntrinsicHeight() { return -1; }
}
