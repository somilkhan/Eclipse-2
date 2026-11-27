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
 * Floating iOS 26 tab bar.
 *
 * <ul>
 *   <li><b>Glass body.</b> API 33+ with the Liquid-Glass-Android library available
 *       ({@link LiquidSupport}): a library {@code LiquidGlassView} refracts the scrolling content
 *       (real lens: bevel refraction, dispersion, specular rim). Otherwise, and instantly if the
 *       library ever throws, the in-house {@link BackdropBlurLayout} glass (blur + rim refraction
 *       approximation + specular rim).</li>
 *   <li><b>Lens selection.</b> A capsule "platter" marks the selected tab; on library devices a
 *       clear library lens sits on it and magnifies/bends the icon underneath. It glides between
 *       tabs on two springs (leading edge first, trailing edge follows = liquid stretch), swells
 *       while a finger is down and can be dragged across the bar; release snaps to the nearest tab.</li>
 *   <li><b>Minimise on scroll.</b> {@link #setMinimized(boolean)} shrinks the bar to a compact
 *       capsule at the leading edge showing only the selected icon (iOS 26
 *       {@code tabBarMinimizeBehavior = .onScrollDown}); tapping it restores the bar. Never minimises
 *       with TalkBack / Switch Access on; with "Remove animations" changes are instant.</li>
 *   <li>Real tabs for accessibility: each item is focusable, announced as "Tab, n of m, selected".</li>
 * </ul>
 */
public class GlassNavPill extends BackdropBlurLayout {

    public interface OnItemSelectedListener { void onItemSelected(@IdRes int id); }

    private final LinearLayout row;
    private final List<ItemView> items = new ArrayList<>();
    private final Paint platterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lensRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF indicatorRect = new RectF();
    private final RectF glass = new RectF();
    private final FloatValueHolder left = new FloatValueHolder();
    private final FloatValueHolder right = new FloatValueHolder();
    private final FloatValueHolder minimizeHolder = new FloatValueHolder(0f);
    private final FloatValueHolder pressHolder = new FloatValueHolder(0f);
    private final SpringAnimation leftSpring;
    private final SpringAnimation rightSpring;
    private final SpringAnimation minimizeSpring;
    private final SpringAnimation pressSpring;
    private final float inset;
    private final float density;
    private final int touchSlop;
    private final boolean night;
    private boolean indicatorPlaced;
    private int selectedId = View.NO_ID;
    private boolean minimized;
    @Nullable private OnItemSelectedListener listener;

    // Library lens (API 33+), null when the fallback glass is in use.
    @Nullable private SafeLiquidGlassView body;
    @Nullable private SafeLiquidGlassView droplet;
    @Nullable private View blurSource;

    // Lens drag
    private float downX;
    private boolean dragging;
    private boolean pressing;

    public GlassNavPill(@NonNull Context context) { this(context, null); }

    public GlassNavPill(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        inset = 4 * density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        platterPaint.setColor(ContextCompat.getColor(context, R.color.ie_glass_indicator));
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
            s.setSpring(new SpringForce().setDampingRatio(0.74f).setStiffness(SpringForce.STIFFNESS_MEDIUM));
            s.addUpdateListener((a, v, vel) -> onLensMoved());
        }
        minimizeSpring = new SpringAnimation(minimizeHolder);
        minimizeSpring.setSpring(new SpringForce().setDampingRatio(0.82f).setStiffness(420f));
        minimizeSpring.setMinimumVisibleChange(0.002f);
        minimizeSpring.addUpdateListener((a, v, vel) -> applyMinimize());
        pressSpring = new SpringAnimation(pressHolder);
        pressSpring.setSpring(new SpringForce().setDampingRatio(0.6f).setStiffness(600f));
        pressSpring.setMinimumVisibleChange(0.002f);
        pressSpring.addUpdateListener((a, v, vel) -> onLensMoved());

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
            b.setEnableDynamicBackground(true);
            b.setCornerRadius(999f);
            b.setOverLight(!night);
            b.setEnableEdgeHighlight(true);
            b.setGlassTint(night ? 0x401C1C1E : 0x66FFFFFF);
            b.setOnBrokenListener(this::fallBackToInHouseGlass);
            b.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(b, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            SafeLiquidGlassView d = new SafeLiquidGlassView(getContext());
            d.setEnablePressEffect(false);
            d.setEnableDynamicBackground(true);
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

            body = b;
            droplet = d;
            if (blurSource != null) b.setBackdropSource(blurSource);
            setGlassDrawingEnabled(false);
        } catch (Throwable t) {
            LiquidSupport.markBroken(t);
            fallBackToInHouseGlass();
        }
    }

    private void fallBackToInHouseGlass() {
        if (body != null) removeView(body);
        if (droplet != null) removeView(droplet);
        body = null;
        droplet = null;
        setGlassDrawingEnabled(true);
        invalidate();
    }

    /** True when the library lens is drawing this bar (false = in-house fallback glass). */
    public boolean usesLibraryLens() { return body != null && !body.hasFailed(); }

    @Override
    public void setBlurSource(@Nullable View source) {
        super.setBlurSource(source);
        blurSource = source;
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
                if (minimized) { setMinimized(false); return; }
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
            onLensMoved();
            return;
        }
        boolean movingRight = l > left.getValue();
        // Leading edge snaps a little faster than the trailing one → liquid stretch.
        (movingRight ? rightSpring : leftSpring).getSpring().setStiffness(760f);
        (movingRight ? leftSpring : rightSpring).getSpring().setStiffness(330f);
        leftSpring.animateToFinalPosition(l);
        rightSpring.animateToFinalPosition(r);
    }

    private void onLensMoved() {
        syncDroplet();
        invalidate();
    }

    /** Lens rectangle incl. press swell (view coordinates). */
    private void lensRect(RectF out) {
        float top = row.getTop() + inset * 0.5f, bottom = row.getBottom() - inset * 0.5f;
        out.set(left.getValue(), top, right.getValue(), bottom);
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
        LayoutParams lp = (LayoutParams) droplet.getLayoutParams();
        if (lp.width != w || lp.height != h) {
            lp.width = w;
            lp.height = h;
            droplet.setLayoutParams(lp);
        }
        droplet.setTranslationX(indicatorRect.left);
        droplet.setTranslationY(indicatorRect.top);
        droplet.setAlpha(1f - minimizeHolder.getValue());
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
        float p = minimizeHolder.getValue();
        int w = getWidth(), h = getHeight();
        out.set(0, 0, w, h);
        if (p <= 0f || w == 0) return;
        float compact = compactWidth();
        float cw = w + (compact - w) * p;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) out.left = w - cw;
        else out.right = cw;
    }

    private float compactWidth() {
        return Math.max(getHeight(), 64 * density);
    }

    @Override
    protected void glassOutline(Outline outline) {
        glassRect(glass);
        outline.setRoundRect(Math.round(glass.left), 0, Math.round(glass.right), getHeight(), getHeight() / 2f);
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
        float m = minimizeHolder.getValue();
        if (m >= 0.99f) return;
        lensRect(indicatorRect);
        float rad = indicatorRect.height() / 2f;
        int baseA = Color.alpha(ContextCompat.getColor(getContext(), R.color.ie_glass_indicator));
        float press = pressHolder.getValue();
        platterPaint.setAlpha(Math.round(baseA * (1f - m) * (1f + 0.6f * press)));
        canvas.drawRoundRect(indicatorRect, rad, rad, platterPaint);
        if (body == null) {
            // Fallback lens rim: bright top edge, faint bottom (the library lens draws its own).
            int top = night ? 0x59FFFFFF : 0xE6FFFFFF;
            int bottom = night ? 0x14FFFFFF : 0x40FFFFFF;
            lensRimPaint.setShader(new LinearGradient(0, indicatorRect.top, 0, indicatorRect.bottom,
                    top, bottom, Shader.TileMode.CLAMP));
            lensRimPaint.setAlpha(Math.round(255 * (1f - m) * (0.55f + 0.45f * press)));
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

    /** Shrinks the bar to a compact capsule showing only the selected tab (scroll down). */
    public void setMinimized(boolean min) {
        if (min && Motion.touchExploration(getContext())) min = false;
        if (minimized == min) return;
        minimized = min;
        // Compact bar exposes a single "expand" target to accessibility.
        for (ItemView v : items) {
            v.setImportantForAccessibility(min && v.getId() != selectedId
                    ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        if (Motion.reduceMotion() || !isAttachedToWindow()) {
            minimizeSpring.cancel();
            minimizeHolder.setValue(min ? 1f : 0f);
            applyMinimize();
        } else {
            minimizeSpring.animateToFinalPosition(min ? 1f : 0f);
        }
    }

    private void applyMinimize() {
        float p = minimizeHolder.getValue();
        glassRect(glass);
        float compactCenter = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? getWidth() - compactWidth() / 2f : compactWidth() / 2f;
        ItemView sel = selectedItem();
        for (ItemView v : items) {
            if (v == sel) {
                float itemCenter = row.getLeft() + v.getLeft() + v.getWidth() / 2f;
                v.setTranslationX((compactCenter - itemCenter) * p);
                v.label().setAlpha(1f - Math.min(1f, p * 1.6f));
                v.icon().setTranslationY(v.label().getHeight() / 2f * p);
                v.setAlpha(1f);
            } else {
                v.setTranslationX((compactCenter - (row.getLeft() + v.getLeft() + v.getWidth() / 2f)) * p * 0.6f);
                v.setAlpha(Math.max(0f, 1f - p * 2.2f));
                v.label().setAlpha(1f);
                v.icon().setTranslationY(0f);
            }
        }
        if (body != null) {
            body.layout(Math.round(glass.left), 0, Math.round(glass.right), getHeight());
        }
        syncDroplet();
        invalidateOutline();
        invalidate();
    }

    // ------------------------------------------------------------------ touch: lens drag

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (minimized || minimizeHolder.getValue() > 0.5f) {
            glassRect(glass);
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && !glass.contains(ev.getX(), ev.getY())) {
                return false; // let touches outside the compact capsule reach the content
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (minimized) return true; // the compact capsule is one big "expand" button
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                dragging = false;
                setPressing(true);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(ev.getX() - downX) > touchSlop && indicatorPlaced
                        && lensHit(downX)) {
                    dragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
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

    private boolean lensHit(float x) {
        float slack = 12 * density;
        return x >= left.getValue() - slack && x <= right.getValue() + slack;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (minimized) {
            if (ev.getActionMasked() == MotionEvent.ACTION_UP) setMinimized(false);
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float w = right.getValue() - left.getValue();
                    float l = ev.getX() - w / 2f;
                    float min = row.getLeft() + row.getPaddingLeft();
                    float max = row.getRight() - row.getPaddingRight() - w;
                    l = Math.max(min, Math.min(max, l));
                    leftSpring.cancel();
                    rightSpring.cancel();
                    left.setValue(l);
                    right.setValue(l + w);
                    onLensMoved();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPressing(false);
                if (dragging) {
                    dragging = false;
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

        ItemView(Context ctx, MenuItem mi, ColorStateList tint, int index, int count) {
            super(ctx);
            this.index = index;
            this.count = count;
            setId(mi.getItemId());
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            float d = getResources().getDisplayMetrics().density;
            setMinimumHeight(Math.round(54 * d));
            setPadding(0, Math.round(7 * d), 0, Math.round(6 * d));
            setClickable(true);
            setFocusable(true);
            setContentDescription(mi.getTitle());
            setBackground(null);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

            icon = new ImageView(ctx);
            icon.setImageDrawable(mi.getIcon());
            icon.setImageTintList(tint);
            icon.setDuplicateParentStateEnabled(true);
            icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            int s = Math.round(26 * d);
            addView(icon, new LayoutParams(s, s));

            label = new TextView(ctx);
            label.setText(mi.getTitle());
            label.setTypeface(Fonts.inter(ctx, 500));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            label.setTextColor(tint);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            label.setLetterSpacing(0.01f);
            label.setDuplicateParentStateEnabled(true);
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 8, 10, 1, TypedValue.COMPLEX_UNIT_SP);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(14 * d));
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
            label.setTypeface(Fonts.inter(getContext(), c ? 600 : 500));
        }

        @Override public void toggle() { setChecked(!checked); }

        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            int[] s = super.onCreateDrawableState(extraSpace + 1);
            if (checked) mergeDrawableStates(s, CHECKED);
            return s;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!Motion.reduceMotion()) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    icon.animate().scaleX(0.86f).scaleY(0.86f).setDuration(110).start();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    icon.animate().scaleX(1f).scaleY(1f).setDuration(320)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2.4f)).start();
                }
            }
            return super.onTouchEvent(e);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.Tab");
            info.setCheckable(false);
            info.setSelected(checked);
            info.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(0, 1, index, 1, false, checked));
        }
    }
}
