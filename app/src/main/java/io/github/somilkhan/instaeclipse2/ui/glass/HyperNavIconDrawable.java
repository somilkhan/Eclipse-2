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
import android.os.SystemClock;

/**
 * Small line icons and gestures adapted from HyperSpring's current Icons drawable.
 * Only the four companion navigation glyphs are included; launcher/System UI icons are not.
 */
final class HyperNavIconDrawable extends Drawable {
    static final int HOME = 0, FEATURES = 1, LOGS = 2, HELP = 3;
    private final int kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix transform = new Matrix();
    private long started = -1L;
    private int color;

    HyperNavIconDrawable(int kind, int color) {
        this.kind = kind;
        this.color = color;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    void setColor(int color) {
        this.color = color;
        invalidateSelf();
    }

    void play() {
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        started = SystemClock.uptimeMillis();
        scheduleSelf(this::invalidateSelf, started + 16L);
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        android.graphics.Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        float t = started < 0 ? 1f : Math.min(1f, (SystemClock.uptimeMillis() - started) / 620f);
        if (t >= 1f && started >= 0) started = -1L;
        paint.setColor(color);
        float scale = Math.min(bounds.width(), bounds.height()) / 24f;
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(scale, scale);
        transform.reset();
        gesture(t);
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
            case FEATURES:
                path.moveTo(4, 7); path.lineTo(20, 7);
                path.moveTo(4, 17); path.lineTo(20, 17);
                path.addCircle(9 + wave(t, 0, 1) * 5f, 7, 2.2f, Path.Direction.CW);
                path.addCircle(15 - wave(t, 0.16f, 1) * 5f, 17, 2.2f, Path.Direction.CW);
                break;
            case LOGS:
                for (int i = 0; i < 3; i++) {
                    float k = reveal(t, 0.10f + i * 0.12f);
                    path.addCircle(5, 7 + i * 5, 1.1f, Path.Direction.CW);
                    path.moveTo(9, 7 + i * 5);
                    path.lineTo(9 + 10 * k, 7 + i * 5);
                }
                break;
            default:
                path.addCircle(12, 12, 8.5f, Path.Direction.CW);
                path.moveTo(12, 11); path.lineTo(12, 16.5f);
                path.addCircle(12, 7.5f - 1.5f * bounce(t), 0.7f, Path.Direction.CW);
                break;
        }
        path.transform(transform);
        canvas.drawPath(path, paint);
        canvas.restore();
        if (started >= 0) scheduleSelf(this::invalidateSelf, SystemClock.uptimeMillis() + 16L);
    }

    private void gesture(float t) {
        if (t >= 1f) return;
        float bounce = bounce(t);
        if (kind == HOME) {
            transform.setScale(1f + 0.10f * bounce, 1f - 0.12f * bounce, 12, 19);
            transform.postTranslate(0, -2.5f * (float) Math.sin(Math.PI * t));
        } else if (kind == HELP) {
            transform.setScale(1f + 0.07f * bounce, 1f + 0.07f * bounce, 12, 12);
        } else if (kind == FEATURES) {
            transform.setRotate(8f * (float) Math.sin(Math.PI * t), 12, 12);
        } else {
            transform.setScale(1f - 0.08f * bounce, 1f + 0.08f * bounce, 12, 12);
        }
    }

    private static float bounce(float t) {
        if (t <= 0f || t >= 1f) return 0f;
        return (float) Math.sin(Math.PI * t) * (1f - 0.18f * t);
    }

    private static float wave(float t, float delay, float end) {
        if (t <= delay) return 0f;
        float p = Math.min(1f, (t - delay) / (end - delay));
        return (float) Math.sin(Math.PI * p);
    }

    private static float reveal(float t, float delay) {
        return Math.max(0f, Math.min(1f, (t - delay) / 0.42f));
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return -1; }
    @Override public int getIntrinsicHeight() { return -1; }
}
