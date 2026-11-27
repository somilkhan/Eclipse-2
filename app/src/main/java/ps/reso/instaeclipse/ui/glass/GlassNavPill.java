package ps.reso.instaeclipse.ui.glass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

import ps.reso.instaeclipse.R;

/**
 * Floating glass navigation pill (replaces BottomNavigationView). Items come from a menu resource,
 * the selection "lens" glides between items on two springs — the leading edge moves first, the
 * trailing edge follows, so it stretches like liquid — and every change gives a selection tick.
 */
public class GlassNavPill extends BackdropBlurLayout {

    public interface OnItemSelectedListener { void onItemSelected(@IdRes int id); }

    private final LinearLayout row;
    private final List<ItemView> items = new ArrayList<>();
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF indicatorRect = new RectF();
    private final FloatValueHolder left = new FloatValueHolder();
    private final FloatValueHolder right = new FloatValueHolder();
    private final SpringAnimation leftSpring;
    private final SpringAnimation rightSpring;
    private final float inset;
    private boolean indicatorPlaced;
    private int selectedId = View.NO_ID;
    private boolean hidden;
    @Nullable private OnItemSelectedListener listener;

    public GlassNavPill(@NonNull Context context) { this(context, null); }

    public GlassNavPill(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        inset = 5 * d;
        indicatorPaint.setColor(ContextCompat.getColor(context, R.color.ie_glass_indicator));

        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = Math.round(inset);
        row.setPadding(pad, pad, pad, pad);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        leftSpring = new SpringAnimation(left);
        rightSpring = new SpringAnimation(right);
        for (SpringAnimation s : new SpringAnimation[]{leftSpring, rightSpring}) {
            s.setSpring(new SpringForce().setDampingRatio(0.78f).setStiffness(SpringForce.STIFFNESS_MEDIUM));
            s.addUpdateListener((a, v, vel) -> invalidate());
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassNavPill);
        int menuRes = a.getResourceId(R.styleable.GlassNavPill_ieMenu, 0);
        a.recycle();
        if (menuRes != 0) inflateMenu(menuRes);
    }

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
            v.setOnClickListener(x -> select(v.getId(), true, true));
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
        if (id == selectedId) return; // reselect = no-op (keeps scroll/search state)
        ItemView target = null;
        for (ItemView v : items) if (v.getId() == id) target = v;
        if (target == null) return;
        selectedId = id;
        for (ItemView v : items) v.setChecked(v == target);
        if (fromUser) Motion.selectionTick(this);
        moveIndicator(fromUser && !Motion.reduceMotion() && indicatorPlaced);
        if (notify && listener != null) listener.onItemSelected(id);
    }

    private void moveIndicator(boolean animate) {
        ItemView t = null;
        for (ItemView v : items) if (v.getId() == selectedId) t = v;
        if (t == null || t.getWidth() == 0) return;
        float l = row.getLeft() + t.getLeft();
        float r = row.getLeft() + t.getRight();
        if (!animate) {
            leftSpring.cancel();
            rightSpring.cancel();
            left.setValue(l);
            right.setValue(r);
            indicatorPlaced = true;
            invalidate();
            return;
        }
        boolean movingRight = l > left.getValue();
        // Leading edge snaps a little faster than the trailing one → liquid stretch.
        (movingRight ? rightSpring : leftSpring).getSpring().setStiffness(700f);
        (movingRight ? leftSpring : rightSpring).getSpring().setStiffness(320f);
        leftSpring.animateToFinalPosition(l);
        rightSpring.animateToFinalPosition(r);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed || !indicatorPlaced) moveIndicator(false);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!indicatorPlaced) return;
        float top = row.getTop() + inset, bottom = row.getBottom() - inset;
        indicatorRect.set(left.getValue(), top, right.getValue(), bottom);
        float rad = (bottom - top) / 2f;
        canvas.drawRoundRect(indicatorRect, rad, rad, indicatorPaint);
    }

    /** Slides the pill off-screen (scroll down) or back (scroll up). Never hides for TalkBack. */
    public void setHidden(boolean hide) {
        if (hide && Motion.touchExploration(getContext())) hide = false;
        if (hidden == hide) return;
        hidden = hide;
        float dy = hide ? getHeight() + ((MarginLayoutParams) getLayoutParams()).bottomMargin + Motion.dp(getContext(), 24) : 0;
        if (Motion.reduceMotion()) {
            setTranslationY(dy);
            setAlpha(hide ? 0f : 1f);
            return;
        }
        animate().translationY(dy).alpha(hide ? 0f : 1f)
                .setDuration(hide ? 220 : 320)
                .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.25f, 1f))
                .start();
    }

    public boolean isHidden() { return hidden; }

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

        ItemView(Context ctx, MenuItem mi, ColorStateList tint, int index, int count) {
            super(ctx);
            this.index = index;
            this.count = count;
            setId(mi.getItemId());
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            float d = getResources().getDisplayMetrics().density;
            setMinimumHeight(Math.round(52 * d));
            setPadding(0, Math.round(6 * d), 0, Math.round(5 * d));
            setClickable(true);
            setFocusable(true);
            setContentDescription(mi.getTitle());
            setBackground(null);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

            ImageView icon = new ImageView(ctx);
            icon.setImageDrawable(mi.getIcon());
            icon.setImageTintList(tint);
            icon.setDuplicateParentStateEnabled(true);
            icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            int s = Math.round(24 * d);
            addView(icon, new LayoutParams(s, s));

            TextView label = new TextView(ctx);
            label.setText(mi.getTitle());
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTextColor(tint);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            label.setLetterSpacing(0.01f);
            label.setDuplicateParentStateEnabled(true);
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 8, 11, 1, TypedValue.COMPLEX_UNIT_SP);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(16 * d));
            lp.topMargin = Math.round(3 * d);
            label.setGravity(Gravity.CENTER);
            addView(label, lp);
        }

        @Override public boolean isChecked() { return checked; }

        @Override public void setChecked(boolean c) {
            if (checked == c) return;
            checked = c;
            setSelected(c);
            refreshDrawableState();
            TextView label = (TextView) getChildAt(1);
            label.setTypeface(null, c ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
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
                    animate().scaleX(0.9f).scaleY(0.9f).setDuration(120).start();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    animate().scaleX(1f).scaleY(1f).setDuration(260)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
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
