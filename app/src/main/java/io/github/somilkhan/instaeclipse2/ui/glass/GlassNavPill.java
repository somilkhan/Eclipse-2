package io.github.somilkhan.instaeclipse2.ui.glass;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.PopupMenu;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import io.github.somilkhan.instaeclipse2.R;

/**
 * Single-canvas companion port of HyperSpring GlassBar (Apache-2.0). Eclipse retains its own
 * menu IDs, coral accent, listener API, and dark companion palette; only the four companion tabs
 * and the GlassBar renderer are included. Source: https://github.com/olilio3/HyperSpring.
 */
public class GlassNavPill extends View {
    public interface OnItemSelectedListener { void onItemSelected(@IdRes int id); }

    private static final float BACKDROP_SCALE = 0.5f;
    private static final float BACKDROP_BLUR_DP = 3f;
    private static final float LENS_DP = 28f;
    private static final float CHROMA = 0.6f;
    private static final float SATURATION = 1.5f;
    private static final int CAPSULE_TINT = 0x66121212;
    private static final int FLAT_CAPSULE = 0xF0121212;
    private static final float EDGE_ALPHA = 0.12f;
    private static final float RIM_ALPHA = 0.5f;
    private static final float GLOW_ALPHA = 0.25f;
    private static final float GLOW_RADIUS_DP = 64f;
    private static final float BAR_INSET_X_DP = 12f;
    private static final float BAR_INSET_Y_DP = 12f;
    private static final float BAR_HEIGHT_DP = 66f;
    private static final float PILL_INSET_DP = 4f;
    private static final float PILL_LIFT_DP = 6f;
    private static final float COMPACT_SCALE = 0.86f;
    private static final float COMPACT_DROP_DP = 4f;
    private static final float COMPACT_AFTER_DP = 24f;
    private static final float EXPAND_AFTER_DP = 12f;
    private static final float PRESS_SCALE = 0.02f;
    private static final float SHIFT_X_DP = 5f;
    private static final float SHIFT_Y_DP = 2.5f;
    private static final float STRETCH_X = 0.015f;
    private static final float STRETCH_Y = 0.006f;
    private static final String STATE_SELECTED = "selected";
    private static final int INVALID_VIRTUAL_ID = Integer.MIN_VALUE;

    // Rounded-rectangle edge refraction and diagonal additive rim, directly adapted from GlassBar.
    private static final String CAPSULE_SHADER =
            "uniform shader content;\n" +
            "uniform float2 size;\n" +
            "uniform float radius;\n" +
            "uniform float refractionHeight;\n" +
            "uniform float refractionAmount;\n" +
            "uniform float depth;\n" +
            "uniform float chroma;\n" +
            "float sdRoundedRect(float2 p,float2 hs,float r){float2 q=abs(p)-(hs-float2(r));return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;}\n" +
            "float2 gradRounded(float2 p,float2 hs,float r){float2 q=abs(p)-(hs-float2(r));if(q.x>=0.0||q.y>=0.0)return sign(p)*normalize(max(q,0.0)+0.0001);float x=step(q.y,q.x);return sign(p)*float2(x,1.0-x);}\n" +
            "float circleMap(float x){return 1.0-sqrt(1.0-x*x);}\n" +
            "half4 main(float2 p){float2 hs=size*0.5;float2 c=p-hs;float sd=sdRoundedRect(c,hs,radius);if(-sd>=refractionHeight)return content.eval(p);sd=min(sd,0.0);float d=circleMap(1.0-(-sd)/refractionHeight)*refractionAmount;float gr=min(radius*1.5,min(hs.x,hs.y));float2 grad=normalize(gradRounded(c,hs,gr)+depth*c/max(length(c),0.001));float2 q=p+d*grad;float k=chroma*(c.x*c.y)/(hs.x*hs.y);if(abs(k)<0.001)return content.eval(q);float2 disp=d*grad*k;half4 a=content.eval(q+disp),b=content.eval(q+disp*0.6667),c0=content.eval(q+disp*0.3333),d0=content.eval(q),e=content.eval(q-disp*0.3333),f=content.eval(q-disp*0.6667),g=content.eval(q-disp);half r=(a.r+b.r+c0.r)/3.5+g.r/7.0;half grn=b.g/7.0+(c0.g+d0.g+e.g)/3.5;half bl=(e.b+f.b+g.b)/3.0;half al=(a.a+b.a+c0.a+d0.a+e.a+f.a+g.a)/7.0;return half4(r,grn,bl,al);}\n";
    private static final String RIM_SHADER =
            "uniform float2 size;\n" +
            "uniform float radius;\n" +
            "uniform float2 light;\n" +
            "float sdRoundedRect(float2 p,float2 hs,float r){float2 q=abs(p)-(hs-float2(r));return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;}\n" +
            "float2 gradRounded(float2 p,float2 hs,float r){float2 q=abs(p)-(hs-float2(r));if(q.x>=0.0||q.y>=0.0)return sign(p)*normalize(max(q,0.0)+0.0001);float x=step(q.y,q.x);return sign(p)*float2(x,1.0-x);}\n" +
            "half4 main(float2 p){float2 hs=size*0.5;float gr=min(radius*1.5,min(hs.x,hs.y));float i=abs(dot(gradRounded(p-hs,hs,gr),light));return half4(half(i));}\n";

    private static final class Tab {
        final int id;
        final String label;
        final HyperSpringIcons icon;
        Tab(int id, CharSequence label, HyperSpringIcons icon) {
            this.id = id;
            this.label = label == null ? "" : label.toString();
            this.icon = icon;
        }
    }

    private static final class Spring {
        final double stiffness;
        final double damping;
        double value;
        double velocity;
        double target;
        Spring(double stiffness, double damping) { this.stiffness = stiffness; this.damping = damping; }
        static Spring period(double response, double damping) {
            double w = 2 * Math.PI / response;
            return new Spring(w * w, 2 * damping * w);
        }
        boolean atRest() { return Math.abs(value - target) < 0.001 && Math.abs(velocity) < 0.005; }
        void reset(double next) { value = target = next; velocity = 0; }
        void step(double dt) {
            while (dt > 0) {
                double h = Math.min(dt, 1.0 / 240.0);
                velocity += (-stiffness * (value - target) - damping * velocity) * h;
                value += velocity * h;
                dt -= h;
            }
            if (atRest()) { value = target; velocity = 0; }
        }
    }

    private final float density;
    private final int touchSlop;
    private final boolean night;
    private final List<Tab> tabs = new ArrayList<>(4);
    private final Paint tintPaint = new Paint();
    private final Paint flatPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final RectF pill = new RectF();
    private final RectF clipRect = new RectF();
    private final Path capsulePath = new Path();
    private final Matrix transform = new Matrix();
    private final Matrix inverse = new Matrix();
    private final Matrix glowMatrix = new Matrix();
    private final float[] touchPoint = new float[2];
    private final int[] viewLocation = new int[2];
    private final int[] sourceLocation = new int[2];
    private final int[] contentLocation = new int[2];
    private final Spring travel = Spring.period(0.4, 0.85);
    private final Spring liftH = Spring.period(0.3, 0.55);
    private final Spring liftV = Spring.period(0.3, 0.7);
    private final Spring press = new Spring(500, 36);
    private final Spring shiftX = new Spring(260, 18);
    private final Spring shiftY = new Spring(260, 18);
    private final Spring compact = new Spring(220, 28);
    private final Choreographer.FrameCallback frameCallback = this::onFrame;
    private final AccessibilityNodeProvider accessibilityProvider = new TabNodeProvider();

    private RenderNode backdrop;
    private RuntimeShader lensShader;
    private RuntimeShader rimShader;
    @Nullable private View blurSource;
    private boolean hostActive = true;
    @Nullable private android.view.ViewTreeObserver watchedObserver;
    private final android.view.ViewTreeObserver.OnPreDrawListener backdropWatcher = () -> {
        View source = blurSource;
        if (hostActive && source != null && source.isDirty()) {
            backdropDirty = true;
            if (isShown()) postInvalidateOnAnimation();
        }
        return true;
    };
    private RenderEffect cachedBackdropEffect;
    private int effectW = -1, effectH = -1;
    private int selectedId = View.NO_ID;
    private int selectedIndex;
    private int textColor;
    private int selectedColor;
    private int baseColor;
    private boolean backdropDirty = true;
    private boolean glassFailed;
    private boolean compacted;
    private boolean ticking;
    private long lastFrame = -1L;
    private double stretch;
    private boolean tracking;
    private boolean dragging;
    private float downX, downY, touchX, touchY;
    private int startIndex, lastHaptic;
    private float scrollAcc;
    private int keyboardFocusIndex = -1;
    private int accessibilityFocusIndex = -1;
    @Nullable private OnItemSelectedListener listener;

    public GlassNavPill(@NonNull Context context) { this(context, null); }

    public GlassNavPill(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        textColor = ContextCompat.getColor(context, R.color.ie_nav_item);
        selectedColor = ContextCompat.getColor(context, R.color.ie_primary);
        baseColor = ContextCompat.getColor(context, R.color.ie_mesh_base);
        tintPaint.setColor(CAPSULE_TINT);
        flatPaint.setColor(FLAT_CAPSULE);
        float stroke = (float) Math.ceil(0.5f * density) * 2f;
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(stroke);
        edgePaint.setColor(Color.WHITE);
        edgePaint.setAlpha(Math.round(255 * EDGE_ALPHA));
        pillPaint.setColor(0x29FFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(12f * density);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        glowPaint.setShader(new RadialGradient(0, 0, GLOW_RADIUS_DP * density,
                Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        if (Build.VERSION.SDK_INT >= 29) backdrop = new RenderNode("eclipseGlassBarBackdrop");
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                lensShader = new RuntimeShader(CAPSULE_SHADER);
                rimShader = new RuntimeShader(RIM_SHADER);
                rimPaint.setStyle(Paint.Style.STROKE);
                rimPaint.setStrokeWidth(stroke);
                rimPaint.setBlendMode(BlendMode.PLUS);
                rimPaint.setShader(rimShader);
                rimPaint.setAlpha(Math.round(255 * RIM_ALPHA));
                rimPaint.setMaskFilter(new BlurMaskFilter(0.25f * density, BlurMaskFilter.Blur.NORMAL));
            } catch (Throwable ignored) {
                lensShader = null;
                rimShader = null;
                rimPaint.setShader(null);
            }
        }
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        int menuRes = 0;
        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassNavPill);
            menuRes = a.getResourceId(R.styleable.GlassNavPill_ieMenu, 0);
            a.recycle();
        }
        if (menuRes != 0) inflateMenu(menuRes);
        if (tabs.isEmpty()) installDefaultTabs();
        updateSelectedId(selectedIndex);
    }

    /** Total measured height for the reference 66dp capsule plus its 12dp vertical glass margin. */
    public static int heightFor(@NonNull Context context) {
        return Math.round((BAR_HEIGHT_DP + 2f * BAR_INSET_Y_DP) * context.getResources().getDisplayMetrics().density);
    }

    private void installDefaultTabs() {
        addTab(R.id.nav_home, getResources().getString(R.string.home), HyperSpringIcons.HOME);
        addTab(R.id.nav_features, getResources().getString(R.string.features), HyperSpringIcons.SLIDERS);
        addTab(R.id.nav_logs, getResources().getString(R.string.logging), HyperSpringIcons.LIST);
        addTab(R.id.nav_help, getResources().getString(R.string.help), HyperSpringIcons.INFO);
    }

    /** Inflate Eclipse's resource menu without creating child Views. */
    public void inflateMenu(int menuRes) {
        tabs.clear();
        Menu menu = new PopupMenu(getContext(), this).getMenu();
        new android.view.MenuInflater(getContext()).inflate(menuRes, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int kind = iconKindFor(item.getItemId());
            addTab(item.getItemId(), item.getTitle(), kind);
        }
        if (tabs.isEmpty()) {
            selectedIndex = 0;
            selectedId = View.NO_ID;
        } else {
            int found = indexOf(selectedId);
            if (found < 0) found = 0;
            selectedIndex = found;
            updateSelectedId(found);
            snapTo(found);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        requestLayout();
        invalidate();
    }

    private int iconKindFor(int id) {
        if (id == R.id.nav_features) return HyperSpringIcons.SLIDERS;
        if (id == R.id.nav_logs) return HyperSpringIcons.LIST;
        if (id == R.id.nav_help) return HyperSpringIcons.INFO;
        return HyperSpringIcons.HOME;
    }

    private void addTab(int id, CharSequence label, int iconKind) {
        HyperSpringIcons icon = new HyperSpringIcons(iconKind, textColor);
        icon.setCallback(this);
        tabs.add(new Tab(id, label, icon));
    }

    private void updateSelectedId(int index) {
        if (index >= 0 && index < tabs.size()) selectedId = tabs.get(index).id;
    }

    private int indexOf(int id) {
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id == id) return i;
        return -1;
    }

    public void setBlurSource(@Nullable View source) {
        stopWatchingBackdrop();
        blurSource = source;
        backdropDirty = true;
        glassFailed = false;
        if (backdrop != null && source != null && isAttachedToWindow()) startWatchingBackdrop();
        invalidate();
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener value) { listener = value; }

    @IdRes public int getSelectedItemId() { return selectedId; }

    /** Programmatic selection notifies the existing listener just like a normal selected tab. */
    public void setSelectedItemId(@IdRes int id) { select(id, true, false); }

    private void select(int id, boolean notify, boolean fromUser) {
        int index = indexOf(id);
        if (index < 0) return;
        if (index == selectedIndex) {
            if (fromUser && !reduceMotion()) animateTo(index);
            return;
        }
        selectedIndex = index;
        selectedId = id;
        if (fromUser) Motion.selectionTick(this);
        if (ValueAnimator.areAnimatorsEnabled()) {
            tabs.get(index).icon.play();
            compact.target = 0;
            compacted = false;
            travel.target = index;
            liftH.target = liftV.target = 1;
            kick();
        } else {
            snapTo(index);
            compact.reset(0);
            compacted = false;
        }
        if (notify && listener != null) listener.onItemSelected(id);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        invalidate();
    }

    private boolean reduceMotion() { return !ValueAnimator.areAnimatorsEnabled(); }

    public void setMinimized(boolean minimized) {
        if (minimized && (Motion.touchExploration(getContext()) || tracking)) minimized = false;
        if (compacted == minimized) return;
        compacted = minimized;
        double target = minimized ? 1d : 0d;
        compact.target = target;
        if (reduceMotion() || !isAttachedToWindow()) {
            compact.reset(target);
            invalidate();
        } else kick();
    }

    public boolean isMinimized() { return compacted; }

    /** HyperSpring scroll accumulation: compact after 24dp down, restore after 12dp up. */
    public void onPageScroll(int y, int dy) {
        if (y <= 0) {
            scrollAcc = 0;
            setMinimized(false);
            return;
        }
        if (dy == 0) return;
        if (Math.signum(dy) != Math.signum(scrollAcc)) scrollAcc = 0;
        scrollAcc += dy;
        if (scrollAcc > COMPACT_AFTER_DP * density) setMinimized(true);
        else if (scrollAcc < -EXPAND_AFTER_DP * density) setMinimized(false);
    }

    /** Stop/destroy handoff: cancel the shared callback and settle without replay on return. */
    public void stopMotion() {
        cancelFrameLoop();
        travel.reset(travel.target);
        liftH.reset(0);
        liftV.reset(0);
        press.reset(0);
        shiftX.reset(0);
        shiftY.reset(0);
        compact.reset(compacted ? 1 : 0);
        stretch = 0;
        dragging = tracking = false;
        for (Tab tab : tabs) tab.icon.finish();
        invalidate();
    }

    /** Activity lifecycle handoff: stop callbacks and release the recorded backdrop display list. */
    public void onHostStop() {
        hostActive = false;
        stopMotion();
        stopWatchingBackdrop();
        releaseBackdrop();
    }

    /** Resume static backdrop tracking after the owning Activity returns to the foreground. */
    public void onHostStart() {
        hostActive = true;
        if (blurSource != null && isAttachedToWindow()) startWatchingBackdrop();
        backdropDirty = true;
        invalidate();
        if (reduceMotion()) stopMotion();
        else if (hasPendingMotion()) kick();
    }

    private void releaseBackdrop() {
        if (backdrop != null && Build.VERSION.SDK_INT >= 29) {
            backdrop.discardDisplayList();
            if (Build.VERSION.SDK_INT >= 31) backdrop.setRenderEffect(null);
        }
        cachedBackdropEffect = null;
        effectW = effectH = -1;
        backdropDirty = true;
    }

    private void animateTo(int index) {
        if (reduceMotion()) { snapTo(index); return; }
        travel.target = index;
        liftH.target = liftV.target = 1;
        kick();
    }

    private boolean hasPendingMotion() {
        if (dragging || !travel.atRest() || !liftH.atRest() || !liftV.atRest()
                || !press.atRest() || !shiftX.atRest() || !shiftY.atRest() || !compact.atRest()) return true;
        long now = android.os.SystemClock.uptimeMillis();
        for (Tab tab : tabs) if (tab.icon.isAnimating(now)) return true;
        return false;
    }

    private void snapTo(int index) {
        travel.reset(index);
        liftH.reset(0);
        liftV.reset(0);
        stretch = 0;
        invalidate();
    }

    private void kick() {
        if (ticking || !hostActive || !isAttachedToWindow()) return;
        ticking = true;
        lastFrame = -1L;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void onFrame(long now) {
        if (!ticking) return;
        if (!hostActive || !isAttachedToWindow()) {
            stopMotion();
            return;
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            stopMotion();
            return;
        }
        double dt = lastFrame < 0L ? 0d : Math.min((now - lastFrame) / 1e9, 0.1d);
        lastFrame = now;
        float scale = ValueAnimator.getDurationScale();
        if (scale > 0f) dt /= scale;
        boolean moving = step(dt, now);
        invalidate();
        if (moving && isAttachedToWindow()) Choreographer.getInstance().postFrameCallback(frameCallback);
        else { ticking = false; lastFrame = -1L; }
    }

    private boolean step(double dt, long now) {
        if (dragging) {
            double before = travel.value;
            travel.value += (travel.target - travel.value) * (1d - Math.exp(-dt / 0.04d));
            travel.target = travel.value;
            travel.velocity = dt > 0d ? (travel.value - before) / dt : 0d;
        } else {
            travel.step(dt);
            if (Math.abs(travel.target - travel.value) < 0.12) {
                liftH.target = 0;
                liftV.target = 0;
            }
        }
        liftH.step(dt);
        liftV.step(dt);
        float stepDp = tabs.isEmpty() || density <= 0 ? 0f : bar.width() / tabs.size() / density;
        double desiredStretch = Math.min(Math.abs(travel.velocity) * stepDp * 6e-5, 0.12);
        stretch += (desiredStretch - stretch) * (1d - Math.exp(-dt / 0.06d));
        press.step(dt);
        shiftX.step(dt);
        shiftY.step(dt);
        compact.step(dt);
        boolean iconActive = false;
        long uptime = android.os.SystemClock.uptimeMillis();
        for (Tab tab : tabs) iconActive |= tab.icon.isAnimating(uptime);
        boolean pillRest = !dragging && travel.atRest() && liftH.atRest() && liftV.atRest()
                && liftH.target == 0 && stretch < 5e-4;
        if (pillRest) { travel.reset(travel.target); stretch = 0; }
        boolean barRest = press.atRest() && shiftX.atRest() && shiftY.atRest() && compact.atRest();
        return !(pillRest && barRest && !iconActive);
    }

    private void barMatrix(Matrix out) {
        float p = (float) press.value;
        float sx = (float) shiftX.value;
        float sy = (float) shiftY.value;
        float c = (float) compact.value;
        float stretchX = Math.abs(sx) / (SHIFT_X_DP * density) * STRETCH_X;
        float stretchY = Math.abs(sy) / (SHIFT_Y_DP * density) * STRETCH_Y;
        float factor = 1f - (1f - COMPACT_SCALE) * c;
        float scaleX = factor * (1f + PRESS_SCALE * p + stretchX - 0.5f * stretchY);
        float scaleY = factor * (1f + PRESS_SCALE * p + stretchY - 0.25f * stretchX);
        float drop = bar.height() * (1f - factor) / 2f + COMPACT_DROP_DP * density * c;
        out.setScale(scaleX, scaleY, bar.centerX(), bar.centerY());
        out.postTranslate(sx, sy + drop);
    }

    private static float rubber(float distance, float max) {
        if (max <= 0f) return 0f;
        return Math.signum(distance) * max * (1f - 1f / (Math.abs(distance) / max * 0.55f + 1f));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (tabs.isEmpty() || bar.isEmpty()) return false;
        barMatrix(transform);
        if (!transform.invert(inverse)) return false;
        touchPoint[0] = event.getX();
        touchPoint[1] = event.getY();
        inverse.mapPoints(touchPoint);
        float segment = bar.width() / tabs.size();
        boolean animate = ValueAnimator.areAnimatorsEnabled();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!bar.contains(touchPoint[0], touchPoint[1])) return false;
                tracking = true;
                dragging = false;
                downX = event.getX();
                downY = event.getY();
                touchX = touchPoint[0];
                touchY = touchPoint[1];
                setMinimized(false);
                if (animate) {
                    press.target = 1;
                    shiftX.target = shiftY.target = 0;
                    kick();
                } else {
                    press.reset(1);
                    shiftX.reset(0);
                    shiftY.reset(0);
                    invalidate();
                }
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (!tracking) return false;
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                touchX = touchPoint[0];
                touchY = touchPoint[1];
                if (animate) {
                    shiftX.target = rubber(dx, SHIFT_X_DP * density);
                    shiftY.target = rubber(dy, SHIFT_Y_DP * density);
                }
                if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) && tabs.size() > 1) {
                    dragging = true;
                    startIndex = lastHaptic = selectedIndex;
                    travel.target = travel.value;
                    travel.velocity = 0;
                    liftH.target = liftV.target = 1;
                }
                if (dragging) {
                    float resistedDx = dx;
                    if ((dx > 0 && startIndex == tabs.size() - 1) || (dx < 0 && startIndex == 0)) resistedDx *= 0.33f;
                    float pos = startIndex + resistedDx / segment;
                    travel.target = Math.max(0f, Math.min(tabs.size() - 1f, pos));
                    int near = Math.round(Math.max(0f, Math.min(tabs.size() - 1f, pos)));
                    if (near != lastHaptic) {
                        Motion.selectionTick(this);
                        lastHaptic = near;
                    }
                    if (!animate) travel.reset((float) travel.target);
                }
                kick();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!tracking) return false;
                tracking = false;
                press.target = shiftX.target = shiftY.target = 0;
                int index;
                boolean cancelled = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
                if (cancelled) index = selectedIndex;
                else if (dragging) {
                    float steps = (event.getX() - downX) / segment;
                    int jump = steps > 0.3f ? Math.max(1, Math.round(steps))
                            : steps < -0.3f ? Math.min(-1, Math.round(steps)) : 0;
                    index = Math.max(0, Math.min(tabs.size() - 1, startIndex + jump));
                } else {
                    index = Math.max(0, Math.min(tabs.size() - 1, (int) ((touchPoint[0] - bar.left) / segment)));
                }
                boolean wasDrag = dragging;
                dragging = false;
                if (!cancelled && index != selectedIndex) {
                    if (!wasDrag) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    select(tabs.get(index).id, true, false);
                } else if (wasDrag) {
                    travel.target = selectedIndex;
                    liftH.target = liftV.target = 0;
                }
                if (!animate) {
                    press.reset(0);
                    shiftX.reset(0);
                    shiftY.reset(0);
                    snapTo(selectedIndex);
                } else kick();
                if (!cancelled && !wasDrag) performClick();
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            default:
                return tracking;
        }
    }

    private void startWatchingBackdrop() {
        if (!hostActive || blurSource == null || watchedObserver != null || !blurSource.isAttachedToWindow()) return;
        watchedObserver = blurSource.getViewTreeObserver();
        watchedObserver.addOnPreDrawListener(backdropWatcher);
    }

    private void stopWatchingBackdrop() {
        if (watchedObserver == null) return;
        if (watchedObserver.isAlive()) watchedObserver.removeOnPreDrawListener(backdropWatcher);
        watchedObserver = null;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (hostActive && blurSource != null) startWatchingBackdrop();
        if (reduceMotion()) stopMotion();
    }

    @Override protected void onDetachedFromWindow() {
        stopWatchingBackdrop();
        stopMotion();
        releaseBackdrop();
        super.onDetachedFromWindow();
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private void cancelFrameLoop() {
        if (ticking) Choreographer.getInstance().removeFrameCallback(frameCallback);
        ticking = false;
        lastFrame = -1L;
    }

    @Override public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        if (isTabIcon(who)) kick();
        else super.scheduleDrawable(who, what, when);
    }

    @Override public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        if (!isTabIcon(who)) super.unscheduleDrawable(who, what);
    }

    private boolean isTabIcon(Drawable drawable) {
        for (Tab tab : tabs) if (tab.icon == drawable) return true;
        return false;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredW = Math.round(360f * density);
        int width = resolveSize(desiredW, widthMeasureSpec);
        int height = resolveSize(heightFor(getContext()), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padX = BAR_INSET_X_DP * density;
        float padY = BAR_INSET_Y_DP * density;
        bar.set(padX, padY, Math.max(padX, w - padX), Math.max(padY, h - padY));
        capsulePath.rewind();
        capsulePath.addRoundRect(0f, 0f, bar.width(), bar.height(), bar.height() / 2f,
                bar.height() / 2f, Path.Direction.CW);
        if (rimShader != null) {
            rimShader.setFloatUniform("size", bar.width(), bar.height());
            rimShader.setFloatUniform("radius", bar.height() / 2f);
            float angle = (float) Math.toRadians(45);
            rimShader.setFloatUniform("light", (float) Math.cos(angle), (float) Math.sin(angle));
        }
        effectW = effectH = -1;
        cachedBackdropEffect = null;
        backdropDirty = true;
        snapTo(selectedIndex);
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bar.isEmpty() || tabs.isEmpty()) return;
        barMatrix(transform);
        canvas.save();
        canvas.concat(transform);
        drawCapsule(canvas);
        drawPill(canvas);
        drawTabs(canvas);
        canvas.restore();
    }

    private void drawCapsule(Canvas canvas) {
        canvas.save();
        canvas.translate(bar.left, bar.top);
        canvas.clipPath(capsulePath);
        boolean glass = !glassFailed && blurSource != null && backdrop != null
                && canvas.isHardwareAccelerated() && Build.VERSION.SDK_INT >= 31;
        if (glass && backdropDirty) {
            try {
                recordBackdrop();
            } catch (Throwable ignored) {
                glassFailed = true;
                glass = false;
            }
        }
        if (glass) {
            canvas.save();
            canvas.scale(1f / BACKDROP_SCALE, 1f / BACKDROP_SCALE);
            canvas.drawRenderNode(backdrop);
            canvas.restore();
            canvas.drawRect(0, 0, bar.width(), bar.height(), tintPaint);
        } else {
            canvas.drawRect(0, 0, bar.width(), bar.height(), flatPaint);
        }
        float glow = Math.max(0f, Math.min(1f, (float) press.value)) * GLOW_ALPHA;
        if (glow > 0.004f) {
            glowMatrix.setTranslate(touchX - bar.left, touchY - bar.top);
            glowPaint.getShader().setLocalMatrix(glowMatrix);
            glowPaint.setAlpha(Math.round(255 * glow));
            canvas.drawPaint(glowPaint);
        }
        canvas.drawPath(capsulePath, edgePaint);
        if (rimShader != null && !glassFailed) canvas.drawPath(capsulePath, rimPaint);
        canvas.restore();
    }

    private void recordBackdrop() {
        View source = blurSource;
        if (source == null || backdrop == null) return;
        int width = Math.max(1, (int) Math.ceil(bar.width() * BACKDROP_SCALE));
        int height = Math.max(1, (int) Math.ceil(bar.height() * BACKDROP_SCALE));
        backdrop.setPosition(0, 0, width, height);
        RecordingCanvas recording = backdrop.beginRecording(width, height);
        try {
            recording.drawColor(baseColor | 0xFF000000);
            recording.scale(BACKDROP_SCALE, BACKDROP_SCALE);
            recording.translate(-bar.left, -bar.top);
            barMatrix(transform);
            if (!transform.invert(inverse)) inverse.reset();
            recording.concat(inverse);
            getLocationInWindow(viewLocation);
            source.getLocationInWindow(sourceLocation);
            recording.translate(sourceLocation[0] - viewLocation[0] - source.getScrollX(),
                    sourceLocation[1] - viewLocation[1] - source.getScrollY());
            source.draw(recording);
        } finally {
            backdrop.endRecording();
        }
        if (width != effectW || height != effectH || cachedBackdropEffect == null) {
            cachedBackdropEffect = createBackdropEffect(width, height);
            backdrop.setRenderEffect(cachedBackdropEffect);
            effectW = width;
            effectH = height;
        }
        backdropDirty = false;
    }

    private RenderEffect createBackdropEffect(int width, int height) {
        float blur = BACKDROP_BLUR_DP * density * BACKDROP_SCALE;
        ColorMatrix saturation = new ColorMatrix();
        saturation.setSaturation(SATURATION);
        RenderEffect effect = RenderEffect.createChainEffect(
                RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(saturation)),
                RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
        if (lensShader != null && Build.VERSION.SDK_INT >= 33) {
            lensShader.setFloatUniform("size", width, height);
            lensShader.setFloatUniform("radius", height / 2f);
            lensShader.setFloatUniform("refractionHeight", LENS_DP * density * BACKDROP_SCALE);
            lensShader.setFloatUniform("refractionAmount", -LENS_DP * density * BACKDROP_SCALE);
            lensShader.setFloatUniform("depth", 1f);
            lensShader.setFloatUniform("chroma", CHROMA);
            effect = RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(lensShader, "content"), effect);
        }
        return effect;
    }

    private void drawPill(Canvas canvas) {
        if (tabs.isEmpty()) return;
        float segment = bar.width() / tabs.size();
        float position = Math.max(0f, Math.min(tabs.size() - 1f, (float) travel.value));
        float inset = PILL_INSET_DP * density;
        float baseW = segment - 2f * inset;
        float baseH = bar.height() - 2f * inset;
        if (baseW <= 0f || baseH <= 0f) return;
        float liftedH = baseH + PILL_LIFT_DP * density;
        float lh = (float) liftH.value;
        float lv = (float) liftV.value;
        float extension = (float) stretch;
        float width = (baseW + (liftedH * baseW / baseH - baseW) * lh) * (1f + extension);
        float height = (baseH + (liftedH - baseH) * lv) * (1f - extension);
        float centerX = bar.left + (position + 0.5f) * segment;
        float centerY = bar.centerY();
        pill.set(centerX - width / 2f, centerY - height / 2f,
                centerX + width / 2f, centerY + height / 2f);
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, pillPaint);
    }

    private void drawTabs(Canvas canvas) {
        float segment = bar.width() / tabs.size();
        float position = (float) travel.value;
        float iconSize = 24f * density;
        float iconTop = bar.centerY() - iconSize / 2f - 8f * density;
        float baseline = bar.centerY() + 19f * density;
        for (int i = 0; i < tabs.size(); i++) {
            float proximity = Math.max(0f, 1f - Math.abs(position - i));
            int color = blend(textColor, selectedColor, proximity);
            float centerX = bar.left + segment * (i + 0.5f);
            HyperSpringIcons icon = tabs.get(i).icon;
            icon.setColor(color);
            int left = Math.round(centerX - iconSize / 2f);
            int top = Math.round(iconTop);
            icon.setBounds(left, top, Math.round(centerX + iconSize / 2f), Math.round(iconTop + iconSize));
            icon.draw(canvas);
            textPaint.setColor(color);
            String label = tabs.get(i).label;
            canvas.drawText(label, 0, label.length(), centerX, baseline, textPaint);
        }
    }

    private static int blend(int from, int to, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        int alpha = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * a);
        int red = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * a);
        int green = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * a);
        int blue = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * a);
        return Color.argb(alpha, red, green, blue);
    }

    /** Selected pill center transformed into the supplied content-stage coordinate space. */
    @NonNull public android.graphics.PointF selectedCenterIn(@NonNull View content) {
        float segment = tabs.isEmpty() ? 0f : bar.width() / tabs.size();
        float selected = tabs.isEmpty() ? 0f : selectedIndex;
        float[] point = {bar.left + (selected + 0.5f) * segment, bar.centerY()};
        barMatrix(transform);
        transform.mapPoints(point);
        getLocationOnScreen(viewLocation);
        content.getLocationOnScreen(contentLocation);
        if (tabs.isEmpty() || bar.isEmpty()) {
            return new android.graphics.PointF(content.getWidth() / 2f, content.getHeight());
        }
        return new android.graphics.PointF(viewLocation[0] + point[0] - contentLocation[0],
                viewLocation[1] + point[1] - contentLocation[1]);
    }

    @Override public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.TabWidget");
        info.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain(1, tabs.size(), false,
                AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE));
        for (int i = 0; i < tabs.size(); i++) info.addChild(this, i);
    }

    @Nullable @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return accessibilityProvider;
    }

    private final class TabNodeProvider extends AccessibilityNodeProvider {
        @Nullable @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId < 0 || virtualViewId >= tabs.size()) return null;
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setSource(GlassNavPill.this, virtualViewId);
            info.setParent(GlassNavPill.this);
            info.setClassName("android.widget.Tab");
            Tab tab = tabs.get(virtualViewId);
            info.setText(tab.label);
            info.setContentDescription(tab.label);
            info.setPackageName(getContext().getPackageName());
            info.setEnabled(isEnabled());
            info.setFocusable(true);
            info.setFocused(keyboardFocusIndex == virtualViewId);
            info.setCheckable(true);
            info.setChecked(selectedIndex == virtualViewId);
            info.setSelected(selectedIndex == virtualViewId);
            info.setClickable(true);
            info.setVisibleToUser(getVisibility() == VISIBLE && isShown());
            info.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(
                    0, 1, virtualViewId, 1, false, selectedIndex == virtualViewId));
            info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            info.addAction(AccessibilityNodeInfo.ACTION_FOCUS);
            info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
            info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            Rect bounds = virtualBounds(virtualViewId);
            info.setBoundsInParent(bounds);
            int[] location = viewLocation;
            getLocationOnScreen(location);
            Rect screen = new Rect(bounds);
            screen.offset(location[0], location[1]);
            info.setBoundsInScreen(screen);
            return info;
        }

        @Override public boolean performAction(int virtualViewId, int action, @Nullable Bundle arguments) {
            if (virtualViewId < 0 || virtualViewId >= tabs.size()) return false;
            if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                setMinimized(false);
                select(tabs.get(virtualViewId).id, true, true);
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_FOCUS) {
                keyboardFocusIndex = virtualViewId;
                requestFocus();
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_FOCUSED);
                invalidate();
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLEAR_FOCUS) {
                if (keyboardFocusIndex == virtualViewId) keyboardFocusIndex = -1;
                invalidate();
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                if (accessibilityFocusIndex != -1) sendAccessibilityEventForVirtualView(
                        accessibilityFocusIndex, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                accessibilityFocusIndex = virtualViewId;
                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
                if (accessibilityFocusIndex == virtualViewId) {
                    accessibilityFocusIndex = -1;
                    sendAccessibilityEventForVirtualView(virtualViewId,
                            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                }
                return true;
            }
            return false;
        }
    }

    private Rect virtualBounds(int index) {
        float segment = bar.width() / Math.max(1, tabs.size());
        float center = bar.left + (index + 0.5f) * segment;
        float minWidth = 48f * density;
        float width = Math.max(segment, minWidth);
        float left = Math.max(0f, center - width / 2f);
        float right = Math.min(getWidth(), center + width / 2f);
        float minHeight = Math.min(getHeight(), 48f * density);
        float top = Math.max(0f, bar.centerY() - minHeight / 2f);
        Rect bounds = new Rect(Math.round(left), Math.round(top), Math.round(right), Math.round(top + minHeight));
        return bounds;
    }

    private void sendAccessibilityEventForVirtualView(int id, int eventType) {
        android.view.accessibility.AccessibilityManager manager = (android.view.accessibility.AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null || !manager.isEnabled()) return;
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        event.setPackageName(getContext().getPackageName());
        event.setClassName("android.widget.Tab");
        event.setSource(this, id);
        if (id >= 0 && id < tabs.size()) event.getText().add(tabs.get(id).label);
        if (getParent() != null) getParent().requestSendAccessibilityEvent(this, event);
    }

    @Override public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!tabs.isEmpty()) {
                    int base = keyboardFocusIndex >= 0 ? keyboardFocusIndex : selectedIndex;
                    int delta = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1;
                    int next = Math.max(0, Math.min(tabs.size() - 1, base + delta));
                    keyboardFocusIndex = next;
                    setMinimized(false);
                    select(tabs.get(next).id, true, true);
                    sendAccessibilityEventForVirtualView(next, AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    return true;
                }
            }
            if ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_SPACE) && !tabs.isEmpty()) {
                int index = keyboardFocusIndex >= 0 ? keyboardFocusIndex : selectedIndex;
                setMinimized(false);
                select(tabs.get(index).id, true, true);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("super", super.onSaveInstanceState());
        state.putInt(STATE_SELECTED, selectedId);
        return state;
    }

    @Override protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("super"));
            int id = bundle.getInt(STATE_SELECTED, View.NO_ID);
            int index = indexOf(id);
            if (index >= 0) {
                selectedIndex = index;
                selectedId = id;
                snapTo(index);
                for (int i = 0; i < tabs.size(); i++) tabs.get(i).icon.finish();
                invalidate();
            }
        } else super.onRestoreInstanceState(state);
    }
}
