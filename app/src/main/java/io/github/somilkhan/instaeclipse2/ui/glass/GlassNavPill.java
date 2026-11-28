package io.github.somilkhan.instaeclipse2.ui.glass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

import io.github.somilkhan.instaeclipse2.R;

/**
 * HyperSpring-inspired floating glass tabs: spring travel/lift, 2% press swell,
 * an 86% compact state which keeps all four tabs visible, and drag-to-select.
 * Existing Eclipse glass backends and accessible child tabs remain intact.
 * Reference: olilio3/HyperSpring GlassBar, Apache-2.0 (see NOTICE).
 */
public class GlassNavPill extends BackdropBlurLayout {

    public interface OnItemSelectedListener { void onItemSelected(@IdRes int id); }

    private final LinearLayout row;
    private final List<ItemView> items = new ArrayList<>();
    private final Paint platterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lensRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fingerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF indicatorRect = new RectF();
    private final RectF glass = new RectF();
    private final FloatValueHolder left = new FloatValueHolder();
    private final FloatValueHolder right = new FloatValueHolder();
    private final FloatValueHolder minimizeHolder = new FloatValueHolder(0f);
    private final FloatValueHolder pressHolder = new FloatValueHolder(0f);
    private final FloatValueHolder liftHolder = new FloatValueHolder(0f);
    private final SpringAnimation leftSpring;
    private final SpringAnimation rightSpring;
    private final SpringAnimation minimizeSpring;
    private final SpringAnimation pressSpring;
    private final SpringAnimation liftSpring;
    private final float inset;
    private final float density;
    private final int touchSlop;
    private final boolean night;
    private final int platterBaseAlpha;
    private final int unselectedColor;
    private final int selectedColor;
    private boolean indicatorPlaced;
    // Fallback lens rim gradient, rebuilt only when the lens height/position changes.
    private float rimShaderTop = Float.NaN, rimShaderBottom = Float.NaN;
    private int selectedId = View.NO_ID;
    private boolean minimized;
    @Nullable private OnItemSelectedListener listener;

    // Library lens (API 33+), null when the fallback glass is in use.
    @Nullable private SafeLiquidGlassView body;
    @Nullable private SafeLiquidGlassView droplet;
    @Nullable private View blurSource;
    private int dropletBaseW, dropletBaseH;

    // Lens drag
    private float downX, downY, downCenter;
    private int lastDragId = View.NO_ID;
    private boolean dragging;
    private boolean pressing;
    private float touchX, touchY;
    private boolean lensFramePosted;

    public GlassNavPill(@NonNull Context context) { this(context, null); }

    public GlassNavPill(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        inset = 4 * density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int platter = ContextCompat.getColor(context, R.color.ie_glass_indicator);
        platterPaint.setColor(platter);
        platterBaseAlpha = Color.alpha(platter);
        unselectedColor = ContextCompat.getColor(context, R.color.ie_nav_item);
        selectedColor = ContextCompat.getColor(context, R.color.ie_primary);
        lensRimPaint.setStyle(Paint.Style.STROKE);
        lensRimPaint.setStrokeWidth(Math.max(1f, density));
        setRefraction(true);
        setElevation(10 * density);
        if (Build.VERSION.SDK_INT >= 28) {
            setOutlineAmbientShadowColor(night ? Color.BLACK : 0x66000000);
            setOutlineSpotShadowColor(night ? Color.BLACK : 0x55000000);
        }

        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = Math.round(inset);
        row.setPadding(pad, pad, pad, pad);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        leftSpring = new SpringAnimation(left);
        rightSpring = new SpringAnimation(right);
        for (SpringAnimation s : new SpringAnimation[]{leftSpring, rightSpring}) {
            s.setSpring(new SpringForce().setDampingRatio(0.85f).setStiffness(400f));
            s.addUpdateListener((a, v, vel) -> onLensMoved());
            s.addEndListener((a, canceled, v, velocity) -> {
                settleLift();
            });
        }
        minimizeSpring = new SpringAnimation(minimizeHolder);
        minimizeSpring.setSpring(new SpringForce().setDampingRatio(0.82f).setStiffness(420f));
        minimizeSpring.setMinimumVisibleChange(0.002f);
        minimizeSpring.addUpdateListener((a, v, vel) -> applyMinimize());
        pressSpring = new SpringAnimation(pressHolder);
        pressSpring.setSpring(new SpringForce().setDampingRatio(0.6f).setStiffness(600f));
        pressSpring.setMinimumVisibleChange(0.002f);
        pressSpring.addUpdateListener((a, v, vel) -> onLensMoved());

        liftSpring = new SpringAnimation(liftHolder);
        liftSpring.setSpring(new SpringForce().setDampingRatio(0.7f).setStiffness(440f));
        liftSpring.setMinimumVisibleChange(0.002f);
        liftSpring.addUpdateListener((animation, value, velocity) -> onLensMoved());

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassNavPill);
        int menuRes = a.getResourceId(R.styleable.GlassNavPill_ieMenu, 0);
        a.recycle();
        if (menuRes != 0) inflateMenu(menuRes);
        installLibraryLens();
    }

    // ------------------------------------------------------------------ library lens

    private void installLibraryLens() {
        if (!LiquidSupport.available(getContext())) return;
        try {
            SafeLiquidGlassView b = new SafeLiquidGlassView(getContext());
            b.setEnablePressEffect(false);
            // No self-driven redraw loop: with "dynamic background" the library invalidates
            // itself from onDraw, i.e. re-captures the whole screen at the display refresh rate
            // (120 Hz) forever, even when nothing moves. We redraw only when the content under
            // the bar changed (see backdropWatcher) or the lens moves.
            b.setEnableDynamicBackground(false);
            b.setCornerRadius(999f);
            b.setOverLight(!night);
            b.setEnableEdgeHighlight(true);
            b.setGlassTint(0x66121212);
            b.setOnBrokenListener(this::fallBackToInHouseGlass);
            b.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(b, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            SafeLiquidGlassView d = new SafeLiquidGlassView(getContext());
            d.setEnablePressEffect(false);
            d.setEnableDynamicBackground(false);
            d.setEnableBackdropBlur(false); // clear magnifier, like the iOS selection lens
            d.setCornerRadius(999f);
            d.setBevelWidth(8 * density);
            d.setRefractionHeight(5 * density);
            d.setDispersionStrength(0.05f);
            d.setOverLight(!night);
            d.setOnBrokenListener(this::fallBackToInHouseGlass);
            d.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            d.setClickable(false);
            addView(d, new LayoutParams(0, 0, Gravity.TOP | Gravity.LEFT));
            // Sized and placed by syncDroplet(); onLayout re-applies it after FrameLayout's pass.

            body = b;
            droplet = d;
            dropletBaseW = dropletBaseH = 0;
            if (blurSource != null) b.setBackdropSource(blurSource);
            setGlassDrawingEnabled(false);
        } catch (Throwable t) {
            LiquidSupport.markBroken(t);
            fallBackToInHouseGlass();
        }
    }

    private void fallBackToInHouseGlass() {
        watchBackdrop(false);
        if (body != null) removeView(body);
        if (droplet != null) removeView(droplet);
        body = null;
        droplet = null;
        dropletBaseW = dropletBaseH = 0;
        setGlassDrawingEnabled(true);
        invalidate();
    }

    /** True when the library lens is drawing this bar (false = in-house fallback glass). */
    public boolean usesLibraryLens() { return body != null && !body.hasFailed(); }

    /**
     * Redraws the library lens only in frames where the content behind the bar changed (scroll,
     * carousel, tab fade, a switch flipping...). Invalidating the lens never dirties the source,
     * so this can't loop.
     */
    private final android.view.ViewTreeObserver.OnPreDrawListener backdropWatcher = () -> {
        View src = blurSource;
        if (src != null && src.isDirty() && isShown()) invalidateLens();
        return true;
    };
    @Nullable private android.view.ViewTreeObserver watchedObserver;

    private void invalidateLens() {
        if (body != null) body.invalidate();
        if (droplet != null) droplet.invalidate();
    }

    private void watchBackdrop(boolean on) {
        if (watchedObserver != null) {
            if (watchedObserver.isAlive()) watchedObserver.removeOnPreDrawListener(backdropWatcher);
            watchedObserver = null;
        }
        if (on && blurSource != null && body != null && isAttachedToWindow()) {
            watchedObserver = blurSource.getViewTreeObserver();
            watchedObserver.addOnPreDrawListener(backdropWatcher);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        watchBackdrop(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        watchBackdrop(false);
        leftSpring.cancel();
        rightSpring.cancel();
        minimizeSpring.cancel();
        pressSpring.cancel();
        liftSpring.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public void setBlurSource(@Nullable View source) {
        super.setBlurSource(source);
        blurSource = source;
        watchBackdrop(true);
        if (body != null) {
            try {
                body.setBackdropSource(source);
            } catch (Throwable t) {
                LiquidSupport.markBroken(t);
                fallBackToInHouseGlass();
            }
        }
    }

    // ------------------------------------------------------------------ items

    /** Builds one item per entry of {@code menuRes} (public API: a throwaway PopupMenu). */
    public void inflateMenu(int menuRes) {
        row.removeAllViews();
        items.clear();
        Menu menu = new PopupMenu(getContext(), this).getMenu();
        new android.view.MenuInflater(getContext()).inflate(menuRes, menu);
        ColorStateList tint = ContextCompat.getColorStateList(getContext(), R.color.ie_nav_item);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            ItemView v = new ItemView(getContext(), mi, tint, i, menu.size());
            v.setOnClickListener(x -> {
                setMinimized(false);
                select(v.getId(), true, true);
            });
            row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            items.add(v);
        }
        if (!items.isEmpty() && selectedId == View.NO_ID) select(items.get(0).getId(), false, false);
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener l) { listener = l; }

    @IdRes public int getSelectedItemId() { return selectedId; }

    /** Programmatic selection; notifies the listener like a tap (BottomNavigationView parity). */
    public void setSelectedItemId(@IdRes int id) { select(id, true, false); }

    private void select(int id, boolean notify, boolean fromUser) {
        if (id == selectedId) {
            moveIndicator(fromUser && !Motion.reduceMotion() && indicatorPlaced);
            return; // reselect = no-op (keeps scroll/search state)
        }
        ItemView target = null;
        for (ItemView v : items) if (v.getId() == id) target = v;
        if (target == null) return;
        selectedId = id;
        for (ItemView v : items) v.setChecked(v == target);
        if (fromUser) Motion.selectionTick(this);
        moveIndicator(fromUser && !Motion.reduceMotion() && indicatorPlaced);
        if (notify && listener != null) listener.onItemSelected(id);
        applyMinimize();
    }

    @Nullable
    private ItemView selectedItem() {
        for (ItemView v : items) if (v.getId() == selectedId) return v;
        return null;
    }

    private void moveIndicator(boolean animate) {
        ItemView t = selectedItem();
        if (t == null || t.getWidth() == 0) return;
        float l = row.getLeft() + t.getLeft();
        float r = row.getLeft() + t.getRight();
        if (!animate) {
            leftSpring.cancel();
            rightSpring.cancel();
            left.setValue(l);
            right.setValue(r);
            indicatorPlaced = true;
            liftSpring.cancel();
            liftHolder.setValue(0f);
            onLensMoved();
            return;
        }
        liftSpring.animateToFinalPosition(1f);
        boolean movingRight = l > left.getValue();
        // Leading edge snaps a little faster than the trailing one → liquid stretch.
        (movingRight ? rightSpring : leftSpring).getSpring().setStiffness(760f);
        (movingRight ? leftSpring : rightSpring).getSpring().setStiffness(330f);
        leftSpring.animateToFinalPosition(l);
        rightSpring.animateToFinalPosition(r);
    }

    private void settleLift() {
        if (liftSpring != null && !leftSpring.isRunning() && !rightSpring.isRunning()) {
            liftSpring.animateToFinalPosition(0f);
        }
    }

    private void onLensMoved() {
        if (lensFramePosted) return;
        lensFramePosted = true;
        postOnAnimation(() -> {
            lensFramePosted = false;
            applyLensFrame();
        });
    }

    private void applyLensFrame() {
        applyMinimize();
        if (!indicatorPlaced || items.isEmpty()) return;
        float center = (left.getValue() + right.getValue()) * 0.5f;
        for (ItemView item : items) {
            float itemCenter = row.getLeft() + item.getLeft() + item.getWidth() * 0.5f;
            float near = Math.max(0f, 1f - Math.abs(center - itemCenter) / Math.max(1f, item.getWidth()));
            // Quantise the visual blend: the pill itself still moves every frame, but child tint
            // state only changes at perceptible colour steps instead of allocating four new
            // ColorStateLists on every spring tick.
            float stepped = Math.round(near * 32f) / 32f;
            item.setVisualColor(blend(unselectedColor, selectedColor, stepped));
        }
    }

    private static int blend(int from, int to, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * amount);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount);
        return Color.argb(a, r, g, b);
    }

    /** Selected tab center in the requested content coordinate system (also works in RTL). */
    public android.graphics.PointF selectedCenterIn(View content) {
        android.graphics.Rect bounds = new android.graphics.Rect();
        ItemView item = selectedItem();
        int[] origin = new int[2];
        content.getLocationOnScreen(origin);
        if (item != null && item.getGlobalVisibleRect(bounds)) {
            return new android.graphics.PointF(bounds.exactCenterX() - origin[0], bounds.exactCenterY() - origin[1]);
        }
        return new android.graphics.PointF(content.getWidth() / 2f, content.getHeight());
    }

    /** Lens rectangle incl. press swell (view coordinates). */
    private void lensRect(RectF out) {
        float top = row.getTop() + inset * 0.5f, bottom = row.getBottom() - inset * 0.5f;
        out.set(left.getValue() + inset, top, right.getValue() - inset, bottom);
        float lift = Math.max(0f, liftHolder.getValue());
        out.inset(-out.width() * 0.045f * lift, -3f * density * lift);
        float p = pressHolder.getValue();
        if (p > 0f) {
            float gx = out.width() * 0.10f * p, gy = out.height() * 0.12f * p;
            out.inset(-gx, -gy);
        }
    }

    private void syncDroplet() {
        if (droplet == null || !indicatorPlaced) return;
        lensRect(indicatorRect);
        int w = Math.round(indicatorRect.width()), h = Math.round(indicatorRect.height());
        // Establish a real layer once. During spring travel do not measure/layout: that forced a
        // full child hierarchy/layout pass every frame and was the main source of pill hitching.
        if (dropletBaseW == 0 || dropletBaseH == 0 || droplet.getWidth() == 0) {
            droplet.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            droplet.layout(0, 0, w, h);
            dropletBaseW = Math.max(1, w);
            dropletBaseH = Math.max(1, h);
            droplet.setPivotX(0f);
            droplet.setPivotY(0f);
        }
        droplet.setTranslationX(indicatorRect.left);
        droplet.setTranslationY(indicatorRect.top);
        droplet.setScaleX(w / (float) dropletBaseW);
        droplet.setScaleY(h / (float) dropletBaseH);
        float alpha = 1f;
        if (droplet.getAlpha() != alpha) droplet.setAlpha(alpha);
        droplet.invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if ((changed || !indicatorPlaced) && !dragging) moveIndicator(false);
        applyMinimize();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void glassRect(RectF out) {
        out.set(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void glassOutline(Outline outline) {
        outline.setRoundRect(0, 0, getWidth(), getHeight(), getHeight() / 2f);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas); // fallback glass (no-op while the library body draws)
        if (body == null) drawPlatter(canvas);
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        boolean r = super.drawChild(canvas, child, drawingTime);
        if (child == body) drawPlatter(canvas);
        return r;
    }

    /** Selected-tab capsule: soft grey platter + glass rim; swells while pressed. */
    private void drawPlatter(Canvas canvas) {
        if (!indicatorPlaced) return;
        lensRect(indicatorRect);
        float rad = indicatorRect.height() / 2f;
        float press = pressHolder.getValue();
        platterPaint.setAlpha(Math.min(255, Math.round(platterBaseAlpha * (1f + 0.3f * press))));
        canvas.drawRoundRect(indicatorRect, rad, rad, platterPaint);
        if (press > 0.004f) {
            canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            fingerGlowPaint.setShader(new RadialGradient(touchX, touchY, 64f * density,
                    0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            fingerGlowPaint.setAlpha(Math.round(255f * press));
            canvas.drawCircle(touchX, touchY, 64f * density, fingerGlowPaint);
            canvas.restore();
        }
        if (body == null) {
            // Fallback lens rim: bright top edge, faint bottom (the library lens draws its own).
            int top = night ? 0x59FFFFFF : 0xE6FFFFFF;
            int bottom = night ? 0x14FFFFFF : 0x40FFFFFF;
            if (indicatorRect.top != rimShaderTop || indicatorRect.bottom != rimShaderBottom) {
                rimShaderTop = indicatorRect.top;
                rimShaderBottom = indicatorRect.bottom;
                lensRimPaint.setShader(new LinearGradient(0, indicatorRect.top, 0, indicatorRect.bottom,
                        top, bottom, Shader.TileMode.CLAMP));
            }
            lensRimPaint.setAlpha(Math.round(255 * (0.55f + 0.45f * press)));
            float hs = lensRimPaint.getStrokeWidth() / 2f;
            indicatorRect.inset(hs, hs);
            canvas.drawRoundRect(indicatorRect, rad - hs, rad - hs, lensRimPaint);
        }
    }

    // ------------------------------------------------------------------ minimise

    /** Back-compat with the old hide-on-scroll API: "hidden" now means minimised (iOS 26). */
    public void setHidden(boolean hide) { setMinimized(hide); }

    public boolean isHidden() { return minimized; }

    public boolean isMinimized() { return minimized; }

    /** HyperSpring-style compact state: all tab targets stay visible and tappable. */
    public void setMinimized(boolean min) {
        if (min && (Motion.touchExploration(getContext()) || pressing)) min = false;
        if (minimized == min) return;
        minimized = min;
        if (Motion.reduceMotion() || !isAttachedToWindow()) {
            minimizeSpring.cancel();
            minimizeHolder.setValue(min ? 1f : 0f);
            applyMinimize();
        } else {
            minimizeSpring.animateToFinalPosition(min ? 1f : 0f);
        }
    }

    private void applyMinimize() {
        float compact = Math.max(0f, Math.min(1f, minimizeHolder.getValue()));
        float press = Math.max(0f, pressHolder.getValue());
        float scale = (1f - 0.14f * compact) * (1f + 0.02f * press);
        setScaleX(scale);
        setScaleY(scale);
        setTranslationY(4f * density * compact);
        syncDroplet();
        invalidate();
    }

    // ------------------------------------------------------------------ touch: lens drag

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                touchX = downX;
                touchY = downY;
                downCenter = (left.getValue() + right.getValue()) / 2f;
                lastDragId = selectedId;
                dragging = false;
                setMinimized(false);
                setPressing(true);
                return false;
            case MotionEvent.ACTION_MOVE:
                touchX = ev.getX();
                touchY = ev.getY();
                if (!dragging && Math.abs(ev.getX() - downX) > touchSlop && indicatorPlaced
                        && Math.abs(ev.getX() - downX) > Math.abs(ev.getY() - downY)) {
                    dragging = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    liftSpring.animateToFinalPosition(1f);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPressing(false);
                return false;
            default:
                return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float w = right.getValue() - left.getValue();
                    float l = downCenter + ev.getX() - downX - w / 2f;
                    float min = row.getLeft() + row.getPaddingLeft();
                    float max = row.getRight() - row.getPaddingRight() - w;
                    l = Math.max(min, Math.min(max, l));
                    leftSpring.cancel();
                    rightSpring.cancel();
                    left.setValue(l);
                    right.setValue(l + w);
                    ItemView nearest = nearestItem(l + w / 2f);
                    if (nearest != null && nearest.getId() != lastDragId) {
                        lastDragId = nearest.getId();
                        Motion.selectionTick(this);
                    }
                    onLensMoved();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPressing(false);
                if (dragging) {
                    dragging = false;
                    liftSpring.animateToFinalPosition(0f);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    ItemView best = null;
                    float c = (left.getValue() + right.getValue()) / 2f, bestD = Float.MAX_VALUE;
                    for (ItemView v : items) {
                        float vc = row.getLeft() + v.getLeft() + v.getWidth() / 2f;
                        if (Math.abs(vc - c) < bestD) { bestD = Math.abs(vc - c); best = v; }
                    }
                    if (best != null && ev.getActionMasked() == MotionEvent.ACTION_UP) {
                        indicatorPlaced = true;
                        if (best.getId() == selectedId) moveIndicator(!Motion.reduceMotion());
                        else select(best.getId(), true, true);
                    } else {
                        moveIndicator(!Motion.reduceMotion());
                    }
                }
                return true;
            default:
                return true;
        }
    }

    private ItemView nearestItem(float center) {
        ItemView best = null;
        float distance = Float.MAX_VALUE;
        for (ItemView item : items) {
            float d = Math.abs(row.getLeft() + item.getLeft() + item.getWidth() / 2f - center);
            if (d < distance) { distance = d; best = item; }
        }
        return best;
    }

    private void setPressing(boolean down) {
        if (pressing == down) return;
        pressing = down;
        if (Motion.reduceMotion()) {
            pressSpring.cancel();
            pressHolder.setValue(down ? 1f : 0f);
            onLensMoved();
        } else {
            pressSpring.animateToFinalPosition(down ? 1f : 0f);
        }
    }

    // ------------------------------------------------------------------ a11y / state

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain(1, items.size(), false,
                AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE));
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
        b.putParcelable("super", super.onSaveInstanceState());
        b.putInt("selected", selectedId);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle b = (Bundle) state;
            super.onRestoreInstanceState(b.getParcelable("super"));
            int id = b.getInt("selected", View.NO_ID);
            if (id != View.NO_ID) {
                select(id, false, false);
                indicatorPlaced = false;
                requestLayout();
            }
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /** One tab: icon over a short label, ≥48dp, checkable so selector drawables switch. */
    @SuppressLint("ViewConstructor")
    static final class ItemView extends LinearLayout implements Checkable {
        private static final int[] CHECKED = {android.R.attr.state_checked};
        private boolean checked;
        private final int index, count;
        private final ImageView icon;
        private final TextView label;
        private final HyperNavIconDrawable gestureIcon;
        private int visualColor = Integer.MIN_VALUE;

        ItemView(Context ctx, MenuItem mi, ColorStateList tint, int index, int count) {
            super(ctx);
            this.index = index;
            this.count = count;
            setId(mi.getItemId());
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            float d = getResources().getDisplayMetrics().density;
            setMinimumHeight(Math.round(58 * d));
            setPadding(0, Math.round(7 * d), 0, Math.round(6 * d));
            setClickable(true);
            setFocusable(true);
            setContentDescription(mi.getTitle());
            setBackground(null);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

            icon = new ImageView(ctx);
            gestureIcon = new HyperNavIconDrawable(index, Color.WHITE);
            icon.setImageDrawable(gestureIcon);
            icon.setImageTintList(tint);
            icon.setDuplicateParentStateEnabled(true);
            icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            int s = Math.round(24 * d);
            addView(icon, new LayoutParams(s, s));

            label = new TextView(ctx);
            label.setText(mi.getTitle());
            label.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setTextColor(tint);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            label.setLetterSpacing(0.01f);
            label.setDuplicateParentStateEnabled(true);
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 9, 12, 1, TypedValue.COMPLEX_UNIT_SP);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(16 * d));
            lp.topMargin = Math.round(2 * d);
            label.setGravity(Gravity.CENTER);
            addView(label, lp);
        }

        ImageView icon() { return icon; }

        TextView label() { return label; }

        @Override public boolean isChecked() { return checked; }

        @Override public void setChecked(boolean c) {
            if (checked == c) return;
            checked = c;
            setSelected(c);
            refreshDrawableState();
            label.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                    c ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
            if (c) gestureIcon.play();
            animateState(false);
        }

        void setVisualColor(int color) {
            if (visualColor == color) return;
            visualColor = color;
            gestureIcon.setColor(color);
            label.setTextColor(color);
            icon.setImageTintList(ColorStateList.valueOf(color));
            invalidate();
        }

        @Override public void toggle() { setChecked(!checked); }

        /**
         * Keep icon and label motion coordinated with the moving lens. Animations are cancelled
         * before every new state so rapid switching cannot leave a tab half-scaled or faded.
         */
        private void animateState(boolean pressed) {
            // Selection settles at the neutral transform; the coordinated alpha/typeface change
            // carries the selected state without leaving a scale offset after an interruption.
            float scale = pressed ? 0.90f : 1f;
            float iconAlpha = checked ? 1f : 0.82f;
            float labelAlpha = checked ? 1f : 0.86f;
            icon.animate().cancel();
            label.animate().cancel();
            if (Motion.reduceMotion()) {
                icon.setScaleX(scale);
                icon.setScaleY(scale);
                icon.setAlpha(iconAlpha);
                label.setAlpha(labelAlpha);
                return;
            }
            long duration = pressed ? 90L : 180L;
            android.view.animation.Interpolator interpolator = pressed
                    ? new android.view.animation.DecelerateInterpolator()
                    : new android.view.animation.OvershootInterpolator(1.35f);
            icon.animate().scaleX(scale).scaleY(scale).alpha(iconAlpha)
                    .setDuration(duration).setInterpolator(interpolator).start();
            label.animate().alpha(labelAlpha).setDuration(duration)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }

        @Override public void setPressed(boolean pressed) {
            super.setPressed(pressed);
            animateState(pressed);
        }

        @Override protected void onDetachedFromWindow() {
            icon.animate().cancel();
            label.animate().cancel();
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setAlpha(1f);
            label.setAlpha(1f);
            super.onDetachedFromWindow();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animateState(false);
        }

        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            int[] s = super.onCreateDrawableState(extraSpace + 1);
            if (checked) mergeDrawableStates(s, CHECKED);
            return s;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            return super.onTouchEvent(e);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.Tab");
            info.setCheckable(true);
            info.setSelected(checked);
            info.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(0, 1, index, 1, false, checked));
        }
    }
}
