package io.github.somilkhan.instaeclipse2.screenshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import io.github.somilkhan.instaeclipse2.ui.glass.CompanionEntrance;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CompanionEntranceTest {
    private FrameLayout host;
    private FrameLayout page;
    private View title;
    private View navigation;

    @Before public void setUp() {
        durationScale(1f);
        host = new FrameLayout(ApplicationProvider.getApplicationContext());
        page = new FrameLayout(host.getContext());
        title = new View(host.getContext());
        title.setTag("ie_large_title");
        View card = new View(host.getContext());
        page.addView(title, new FrameLayout.LayoutParams(200, 100));
        page.addView(card, new FrameLayout.LayoutParams(200, 120));
        navigation = new View(host.getContext());
        host.addView(page, new FrameLayout.LayoutParams(400, 700));
        host.addView(navigation, new FrameLayout.LayoutParams(400, 80));
        host.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        host.layout(0, 0, 400, 800);
    }

    @After public void tearDown() {
        durationScale(1f);
    }

    @Test public void firstTouchSkipsEntranceAndRestoresCleanProperties() {
        CompanionEntrance entrance = new CompanionEntrance();
        entrance.play(host, page, navigation);
        page.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals(0f, title.getAlpha(), 0f);
        assertEquals(0f, navigation.getAlpha(), 0f);

        host.getChildAt(2).dispatchTouchEvent(
                MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10, 10, 0));
        assertEquals(1f, title.getAlpha(), 0f);
        assertEquals(1f, navigation.getAlpha(), 0f);
        assertEquals(0f, title.getTranslationY(), 0f);
        assertEquals(0f, navigation.getTranslationY(), 0f);
        assertTrue(host.getChildCount() >= 2);
    }

    @Test public void reducedMotionDoesNotInstallEntranceOverlay() {
        durationScale(0f);
        CompanionEntrance entrance = new CompanionEntrance();
        entrance.play(host, page, navigation);
        page.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals(1f, title.getAlpha(), 0f);
        assertEquals(1f, navigation.getAlpha(), 0f);
        assertEquals(2, host.getChildCount());
    }

    @Test public void finishDuringEntranceRestoresPropertiesAndRemovesOverlay() {
        CompanionEntrance entrance = new CompanionEntrance();
        entrance.play(host, page, navigation);
        page.getViewTreeObserver().dispatchOnPreDraw();
        entrance.finish();
        assertEquals(1f, title.getAlpha(), 0f);
        assertEquals(1f, navigation.getAlpha(), 0f);
        assertEquals(2, host.getChildCount());
    }

    private static void durationScale(float scale) {
        ReflectionHelpers.callStaticMethod(ValueAnimator.class, "setDurationScale",
                ReflectionHelpers.ClassParameter.from(float.class, scale));
    }
}
