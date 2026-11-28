package io.github.somilkhan.instaeclipse2.ui.glass;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Companion-only cold-start entrance adapted from HyperSpring's AppEntrance.
 *
 * <p>It operates only on Eclipse's already-inflated companion views. It does not touch Instagram
 * windows, Xposed hooks, launcher surfaces, or System UI.</p>
 */
public final class CompanionEntrance {
    private static final long DURATION_MS = 1_080L;
    private static final float CONTENT_DY_DP = 22f;
    private static final float NAV_DY_DP = 72f;

    private final List<AnimatedView> animated = new ArrayList<>();
    private ViewTreeObserver preDrawSource;
    private ViewTreeObserver.OnPreDrawListener preDrawObserver;
    private ValueAnimator clock;
    private FrameLayout host;
    private View nav;
    private WaveView wave;
    private boolean finished;

    private static final class AnimatedView {
        final View view;
        final boolean title;
        final float alpha;
        final float translationY;
        final float scaleX;
        final float scaleY;

        AnimatedView(View view, boolean title) {
            this.view = view;
            this.title = title;
            this.alpha = view.getAlpha();
            this.translationY = view.getTranslationY();
            this.scaleX = view.getScaleX();
            this.scaleY = view.getScaleY();
        }

        void prepare(float dy) {
            view.setAlpha(0f);
            view.setTranslationY(translationY + dy);
            if (title) {
                view.setScaleX(scaleX * 0.96f);
                view.setScaleY(scaleY * 0.96f);
            }
        }

        void restore() {
            view.setAlpha(alpha);
            view.setTranslationY(translationY);
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
        }
    }

    /** Starts after the first Home layout has real bounds; screenshot runs intentionally skip it. */
    public void play(final FrameLayout root, final View page, final View navigation) {
        finish();
        if (Motion.reduceMotion() || Boolean.getBoolean("instaeclipse.screenshotDir")
                || page == null || navigation == null || !(page instanceof ViewGroup)) return;
        final ViewGroup content = (ViewGroup) page;
        if (content.getChildCount() == 0) return;
        host = root;
        nav = navigation;
        finished = false;
        preDrawObserver = new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (page.getViewTreeObserver().isAlive()) {
                    page.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                preDrawObserver = null;
                start(content);
                return true;
            }
        };
        preDrawSource = page.getViewTreeObserver();
        preDrawSource.addOnPreDrawListener(preDrawObserver);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void start(ViewGroup page) {
        if (finished || host == null || host.getWidth() == 0 || host.getHeight() == 0) return;
        ViewGroup list = page.getChildAt(0) instanceof ViewGroup
                ? (ViewGroup) page.getChildAt(0) : page;
        View title = list.findViewWithTag("ie_large_title");
        if (title != null && title.getVisibility() == View.VISIBLE) {
            animated.add(new AnimatedView(title, true));
        }
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (child == title || child.getVisibility() != View.VISIBLE) continue;
            animated.add(new AnimatedView(child, false));
        }
        if (animated.isEmpty()) return;
        float density = host.getResources().getDisplayMetrics().density;
        for (AnimatedView item : animated) {
            item.prepare((item.title ? -30f : CONTENT_DY_DP) * density);
        }

        final float[] center = centerInHost(title != null ? title : page);
        final float maxRadius = (float) Math.hypot(
                Math.max(center[0], host.getWidth() - center[0]),
                Math.max(center[1], host.getHeight() - center[1])) + 40f * density;
        wave = new WaveView(host.getContext(), center[0], center[1], density);
        ViewGroup stage = host.findViewById(io.github.somilkhan.instaeclipse2.R.id.content_stage);
        if (stage != null) stage.addView(wave, new ViewGroup.LayoutParams(-1, -1));
        nav.setAlpha(0f);
        nav.setTranslationY(NAV_DY_DP * density);

        final View skip = new View(host.getContext());
        skip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        skip.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && clock != null) clock.end();
            return true;
        });
        host.addView(skip, new FrameLayout.LayoutParams(-1, -1));

        clock = ValueAnimator.ofFloat(0f, 1f);
        clock.setDuration(DURATION_MS);
        clock.setInterpolator(null);
        clock.addUpdateListener(a -> frame((Float) a.getAnimatedValue(), maxRadius, density));
        clock.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                frame(1f, maxRadius, density);
                for (AnimatedView item : animated) item.restore();
                animated.clear();
                if (nav != null) {
                    nav.setAlpha(1f);
                    nav.setTranslationY(0f);
                }
                if (skip.getParent() instanceof ViewGroup) {
                    ((ViewGroup) skip.getParent()).removeView(skip);
                }
                if (wave != null && wave.getParent() instanceof ViewGroup) {
                    ((ViewGroup) wave.getParent()).removeView(wave);
                }
                clock = null;
                wave = null;
                host = null;
                nav = null;
            }
        });
        clock.start();
    }

    private void frame(float progress, float maxRadius, float density) {
        for (int i = 0; i < animated.size(); i++) {
            AnimatedView item = animated.get(i);
            float start = item.title ? 0f : 0.08f + Math.min(0.50f, i * 0.055f);
            float p = Math.max(0f, Math.min(1f, (progress - start) / 0.58f));
            float e = Motion.POWER4_OUT.getInterpolation(p);
            item.view.setAlpha(item.alpha * e);
            item.view.setTranslationY(item.translationY
                    + (item.title ? -30f : CONTENT_DY_DP) * density * (1f - e));
            if (item.title) {
                float bounce = Motion.spring((progress - 0.22f) / 0.78f, 0.42f, 0.72f);
                item.view.setScaleX(item.scaleX * (0.96f + 0.04f * bounce));
                item.view.setScaleY(item.scaleY * (0.96f + 0.04f * bounce));
            }
        }
        if (nav != null) {
            float navP = Math.max(0f, Math.min(1f, (progress - 0.58f) / 0.42f));
            float n = Motion.spring(navP * 1.1f, 0.6f, 0.7f);
            nav.setAlpha(n);
            nav.setTranslationY(NAV_DY_DP * density * (1f - n));
            nav.invalidate();
        }
        if (wave != null) {
            wave.setProgress(progress, maxRadius);
            wave.invalidate();
        }
    }

    private float[] centerInHost(View view) {
        int[] viewLocation = new int[2];
        int[] hostLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        host.getLocationOnScreen(hostLocation);
        return new float[]{viewLocation[0] - hostLocation[0] + view.getWidth() * 0.3f,
                viewLocation[1] - hostLocation[1] + view.getHeight() * 0.8f};
    }

    /** Ends the pending entrance and restores every touched property. */
    public void finish() {
        finished = true;
        if (preDrawObserver != null && preDrawSource != null) {
            if (preDrawSource.isAlive()) preDrawSource.removeOnPreDrawListener(preDrawObserver);
        }
        preDrawObserver = null;
        preDrawSource = null;
        if (clock != null) {
            clock.end();
            return;
        }
        for (AnimatedView item : animated) item.restore();
        animated.clear();
        if (wave != null && wave.getParent() instanceof ViewGroup) {
            ((ViewGroup) wave.getParent()).removeView(wave);
        }
        if (nav != null) {
            nav.setAlpha(1f);
            nav.setTranslationY(0f);
        }
        host = null;
        nav = null;
        wave = null;
    }

    private static final class WaveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float cx, cy;
        private float radius, alpha;

        WaveView(android.content.Context context, float cx, float cy, float density) {
            super(context);
            this.cx = cx;
            this.cy = cy;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * density);
            paint.setColor(0xFFFF5F45);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void setProgress(float progress, float maxRadius) {
            float p = Math.max(0f, Math.min(1f, (progress - 0.12f) / 0.78f));
            radius = maxRadius * (1f - (1f - p) * (1f - p));
            alpha = progress < 0.12f ? 0f
                    : 0.30f * (1f - Math.max(0f, Math.min(1f, (p - 0.55f) / 0.45f)));
            paint.setAlpha(Math.round(alpha * 255f));
        }

        @Override protected void onDraw(Canvas canvas) {
            if (radius > 0f && alpha > 0f) canvas.drawCircle(cx, cy, radius, paint);
        }
    }
}
