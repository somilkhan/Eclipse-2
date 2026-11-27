package io.github.somilkhan.instaeclipse2.mods.piko;

import android.view.View;
import android.view.ViewTreeObserver;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * Keeps a view GONE while a condition holds, even if Instagram flips it back to VISIBLE later
 * (state flows and Litho re-binds do that). A pre-draw listener runs before every frame of the
 * view's window: if the view became visible again it is hidden and that frame is skipped, so
 * the view never flashes. The listener is tied to attach/detach, so nothing leaks.
 *
 * <p>Views are tracked in a weak map instead of View tags: tag keys must be app resource ids,
 * which a module can't mint inside Instagram's process.
 */
final class ViewHider {

    private static final Map<View, Guard> GUARDS = Collections.synchronizedMap(new WeakHashMap<>());

    private ViewHider() {}

    /** Hides {@code v} now and for as long as {@code active} returns true. */
    static void keepHidden(View v, BooleanSupplier active) {
        if (v == null) return;
        Guard g = GUARDS.get(v);
        if (g == null) {
            g = new Guard(v, active);
            GUARDS.put(v, g);
            v.addOnAttachStateChangeListener(g);
            if (v.isAttachedToWindow()) g.onViewAttachedToWindow(v);
        } else {
            g.active = active;
        }
        g.apply();
    }

    /** Stops guarding {@code v}; restores VISIBLE only if this class is what hid it. */
    static void release(View v) {
        if (v == null) return;
        Guard g = GUARDS.remove(v);
        if (g == null) return;
        g.active = () -> false;
        v.removeOnAttachStateChangeListener(g);
        g.onViewDetachedFromWindow(v);
        if (g.hidByUs && v.getVisibility() == View.GONE) v.setVisibility(View.VISIBLE);
    }

    static boolean isGuarded(View v) {
        return v != null && GUARDS.containsKey(v);
    }

    private static final class Guard implements View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
        private final View view;
        volatile BooleanSupplier active;
        private ViewTreeObserver observer;
        boolean hidByUs;

        Guard(View view, BooleanSupplier active) {
            this.view = view;
            this.active = active;
        }

        /** True when it had to hide the view. */
        boolean apply() {
            boolean on;
            try { on = active.getAsBoolean(); } catch (Throwable t) { on = false; }
            if (!on) {
                // Feature switched off live: give the view back.
                if (hidByUs && view.getVisibility() == View.GONE) view.setVisibility(View.VISIBLE);
                hidByUs = false;
                return false;
            }
            if (view.getVisibility() == View.GONE) return false;
            view.setVisibility(View.GONE);
            hidByUs = true;
            return true;
        }

        @Override public boolean onPreDraw() {
            return !apply(); // skip only the frame in which we changed something
        }

        @Override public void onViewAttachedToWindow(View v) {
            if (observer != null && observer.isAlive()) return;
            observer = v.getViewTreeObserver();
            observer.addOnPreDrawListener(this);
            apply();
        }

        @Override public void onViewDetachedFromWindow(View v) {
            ViewTreeObserver o = observer;
            observer = null;
            if (o != null && o.isAlive()) o.removeOnPreDrawListener(this);
            else v.getViewTreeObserver().removeOnPreDrawListener(this);
        }
    }
}
