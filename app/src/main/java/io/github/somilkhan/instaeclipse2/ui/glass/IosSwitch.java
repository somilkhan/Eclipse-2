package io.github.somilkhan.instaeclipse2.ui.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.somilkhan.instaeclipse2.R;

/**
 * iOS 26 style toggle: 51x31dp capsule track, 27dp white knob with soft shadow, systemGreen when on.
 *
 * <p>Extends {@link MaterialSwitch} purely so existing code that types rows as MaterialSwitch keeps
 * working (checked state, listeners, accessibility role "switch"); all measuring, drawing and touch
 * handling is our own. While a finger is down the knob stretches and turns into a translucent glass
 * lens (track colour shows through, bright rim and specular highlight), like the Liquid Glass toggle.
 * Position and press use springs; with "Remove animations" on, state changes are instant.</p>
 */
public class IosSwitch extends MaterialSwitch {

    private static final float TRACK_W = 51f, TRACK_H = 31f, KNOB = 27f, INSET = 2f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final int touchSlop;
    private int offColor, onColor;

    /** 0 = off, 1 = on (logical; mirrored for RTL at draw time). */
    private float pos;
    /** 0 = resting, 1 = finger down (stretched glass knob). */
    private float press;

    private SpringAnimation posSpring, pressSpring;
    private float downX, downPos;
    private boolean dragging;
    private boolean ready;

    private static final FloatPropertyCompat<IosSwitch> POS = new FloatPropertyCompat<IosSwitch>("pos") {
        @Override public float getValue(IosSwitch s) { return s.pos * 1000f; }
        @Override public void setValue(IosSwitch s, float v) { s.pos = v / 1000f; s.invalidate(); }
    };
    private static final FloatPropertyCompat<IosSwitch> PRESS = new FloatPropertyCompat<IosSwitch>("press") {
        @Override public float getValue(IosSwitch s) { return s.press * 1000f; }
        @Override public void setValue(IosSwitch s, float v) { s.press = v / 1000f; s.invalidate(); }
    };

    public IosSwitch(Context context) { this(context, null); }

    public IosSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialSwitchStyle);
    }

    public IosSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        // Kill every Material drawable so nothing of SwitchCompat ever paints.
        setThumbDrawable(null);
        setTrackDrawable(null);
        setThumbIconDrawable(null);
        setTrackDecorationDrawable(null);
        setShowText(false);
        setBackground(null);
        setMinHeight(0);
        setMinimumHeight(0);
        setMinWidth(0);
        setMinimumWidth(0);
        setPadding(0, 0, 0, 0);
        setLayerType(View.LAYER_TYPE_NONE, null);
        offColor = ContextCompat.getColor(context, R.color.ie_switch_track_off);
        onColor = ContextCompat.getColor(context, R.color.ie_switch_on);
        knobPaint.setColor(Color.WHITE);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(density);
        pos = isChecked() ? 1f : 0f;
        posSpring = new SpringAnimation(this, POS).setSpring(new SpringForce()
                .setStiffness(700f).setDampingRatio(0.72f));
        pressSpring = new SpringAnimation(this, PRESS).setSpring(new SpringForce()
                .setStiffness(900f).setDampingRatio(0.8f));
        posSpring.setMinimumVisibleChange(1f);
        pressSpring.setMinimumVisibleChange(1f);
        ready = true;
    }

    /** Override the "on" colour (e.g. theme customizer accent). 0 resets to systemGreen. */
    public void setOnColor(int color) {
        onColor = color != 0 ? color : ContextCompat.getColor(getContext(), R.color.ie_switch_on);
        invalidate();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = Math.round(TRACK_W * density) + getPaddingLeft() + getPaddingRight();
        // Keep a 44dp-ish touch target vertically when the parent allows it, track stays 31dp.
        int h = Math.round(TRACK_H * density) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    public void setChecked(boolean checked) {
        boolean changed = checked != isChecked();
        super.setChecked(checked);
        if (!ready) return;
        float target = checked ? 1f : 0f;
        if (!changed && !dragging && pos == target) return;
        animateTo(target);
    }

    private void animateTo(float target) {
        if (Motion.reduceMotion() || !isAttachedToWindow() || getWindowVisibility() != VISIBLE) {
            posSpring.cancel();
            pos = target;
            invalidate();
        } else {
            posSpring.animateToFinalPosition(target * 1000f);
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (!ready) return;
        posSpring.cancel();
        pressSpring.cancel();
        pos = isChecked() ? 1f : 0f;
        press = 0f;
        invalidate();
    }

    private void setPressedVisual(boolean down) {
        if (Motion.reduceMotion()) {
            pressSpring.cancel();
            press = down ? 1f : 0f;
            invalidate();
        } else {
            pressSpring.animateToFinalPosition(down ? 1000f : 0f);
        }
    }

    private boolean rtl() { return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL; }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) return false;
        float travel = (TRACK_W - KNOB - 2 * INSET) * density;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downPos = pos;
                dragging = false;
                setPressed(true);
                setPressedVisual(true);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getX() - downX;
                if (!dragging && Math.abs(dx) > touchSlop) dragging = true;
                if (dragging) {
                    if (rtl()) dx = -dx;
                    posSpring.cancel();
                    pos = Math.max(0f, Math.min(1f, downPos + dx / travel));
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                setPressed(false);
                setPressedVisual(false);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (dragging) {
                    boolean want = pos >= 0.5f;
                    dragging = false;
                    if (want != isChecked()) {
                        setChecked(want);
                        Motion.selectionTick(this);
                    } else {
                        animateTo(want ? 1f : 0f);
                    }
                } else {
                    performClick();
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                setPressedVisual(false);
                dragging = false;
                animateTo(isChecked() ? 1f : 0f);
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        boolean r = super.performClick(); // toggles via CompoundButton
        Motion.selectionTick(this);
        return r;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        final float d = density;
        float w = TRACK_W * d, h = TRACK_H * d;
        float left = (getWidth() - w) / 2f, top = (getHeight() - h) / 2f;
        float p = Math.max(0f, Math.min(1f, pos));
        int track = blend(offColor, onColor, p);
        if (!isEnabled()) track = Color.argb(Math.round(Color.alpha(track) * 0.5f), Color.red(track), Color.green(track), Color.blue(track));
        trackPaint.setColor(track);
        rect.set(left, top, left + w, top + h);
        c.drawRoundRect(rect, h / 2f, h / 2f, trackPaint);

        // Knob: 27dp circle; while pressed it widens to a capsule and grows past the track (glass lens).
        float pr = Math.max(0f, Math.min(1.2f, press));
        float kh = KNOB * d * (1f + 0.22f * pr);
        float kw = KNOB * d * (1f + 0.62f * pr);
        float travel = w - 2 * INSET * d - KNOB * d;
        float logical = rtl() ? 1f - pos : pos;
        float restCx = left + INSET * d + KNOB * d / 2f + travel * logical;
        // Stretch away from the edge it rests against so it stays roughly within the track.
        float cx = restCx + (kw - KNOB * d) / 2f * (1f - 2f * logical);
        float cy = top + h / 2f;
        rect.set(cx - kw / 2f, cy - kh / 2f, cx + kw / 2f, cy + kh / 2f);
        float r = kh / 2f;

        int glassA = Math.round(255 * (1f - 0.72f * Math.min(1f, pr)));
        if (isEnabled()) {
            knobPaint.setShadowLayer(d * (3f + 3f * pr), 0, d * (1.5f + pr), Color.argb(Math.round(48 * (1f - 0.4f * pr)), 0, 0, 0));
        } else {
            knobPaint.clearShadowLayer();
        }
        knobPaint.setColor(Color.argb(glassA, 255, 255, 255));
        c.drawRoundRect(rect, r, r, knobPaint);
        if (pr > 0.02f) {
            // Glass lens: bright rim + top specular sweep.
            rimPaint.setColor(Color.argb(Math.round(230 * Math.min(1f, pr)), 255, 255, 255));
            c.drawRoundRect(rect, r, r, rimPaint);
            specPaint.setShader(new LinearGradient(0, rect.top, 0, rect.centerY(),
                    Color.argb(Math.round(150 * Math.min(1f, pr)), 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            float ins = d * 1.5f;
            rect.inset(ins, ins);
            c.drawRoundRect(rect, r - ins, r - ins, specPaint);
        }
    }

    private static int blend(int a, int b, float t) {
        float u = 1f - t;
        return Color.argb(
                Math.round(Color.alpha(a) * u + Color.alpha(b) * t),
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }
}
