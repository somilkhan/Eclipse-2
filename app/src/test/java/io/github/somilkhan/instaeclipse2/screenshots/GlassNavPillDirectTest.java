package io.github.somilkhan.instaeclipse2.screenshots;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;

import java.time.Duration;

import io.github.somilkhan.instaeclipse2.MainActivity;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.ui.glass.GlassNavPill;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
public class GlassNavPillDirectTest {
    private ActivityController<MainActivity> controller;

    @Before public void setUp() {
        MainActivity.prewarmTabs = false;
        MainActivity.startupEntranceEnabled = false;
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
        durationScale(1f);
        ScreenshotSupport.installFakeInstagram();
    }

    @After public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
        MainActivity.prewarmTabs = true;
        MainActivity.startupEntranceEnabled = true;
        durationScale(1f);
    }

    private MainActivity activity() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1300));
        ScreenshotSupport.applyPhoneInsets(controller.get());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        return controller.get();
    }

    @Test public void selectedPillCenterMapsToContentStageForAllFourTabs() {
        MainActivity activity = activity();
        GlassNavPill nav = activity.findViewById(R.id.bottom_navigation);
        View stage = activity.findViewById(R.id.content_stage);
        nav.setOnItemSelectedListener(null);
        int[] navLocation = new int[2];
        int[] stageLocation = new int[2];
        nav.getLocationOnScreen(navLocation);
        stage.getLocationOnScreen(stageLocation);
        float d = activity.getResources().getDisplayMetrics().density;
        float pillLeft = 12f * d;
        float segment = (nav.getWidth() - 24f * d) / 4f;
        int[] ids = {R.id.nav_home, R.id.nav_features, R.id.nav_logs, R.id.nav_help};
        for (int i = 0; i < ids.length; i++) {
            nav.setSelectedItemId(ids[i]);
            nav.stopMotion();
            PointF center = nav.selectedCenterIn(stage);
            float expectedX = navLocation[0] - stageLocation[0] + pillLeft + segment * (i + 0.5f);
            float expectedY = navLocation[1] - stageLocation[1] + nav.getHeight() / 2f;
            assertEquals("tab " + i + " center x", expectedX, center.x, 1.5f);
            assertEquals("tab " + i + " center y", expectedY, center.y, 1.5f);
        }
    }

    @Test public void oneChoreographerLoopSettlesRapidTravelAndCancelsOnStopOrDetach() {
        MainActivity activity = activity();
        GlassNavPill nav = activity.findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(R.id.nav_features);
        nav.setSelectedItemId(R.id.nav_logs);
        nav.setSelectedItemId(R.id.nav_help);
        nav.setSelectedItemId(R.id.nav_home);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2300));
        assertEquals(R.id.nav_home, nav.getSelectedItemId());
        assertFalse("frame callback should stop when travel, compact, press, and icon gestures settle",
                ReflectionHelpers.getField(nav, "ticking"));

        nav.onHostStop();
        assertFalse(ReflectionHelpers.getField(nav, "ticking"));
        assertNull("host stop removes the backdrop pre-draw watcher", ReflectionHelpers.getField(nav, "watchedObserver"));
        assertNull("host stop discards the cached backdrop effect", ReflectionHelpers.getField(nav, "cachedBackdropEffect"));
        nav.setSelectedItemId(R.id.nav_features);
        assertFalse("a stopped activity must not post a new frame callback", ReflectionHelpers.getField(nav, "ticking"));
        nav.onHostStart();
        assertNotNull("host start rebinds backdrop tracking", ReflectionHelpers.getField(nav, "watchedObserver"));
        assertTrue("pending tab motion resumes on host start", ReflectionHelpers.getField(nav, "ticking"));

        nav.stopMotion();
        assertFalse(ReflectionHelpers.getField(nav, "ticking"));
        nav.setSelectedItemId(R.id.nav_home);
        assertTrue(ReflectionHelpers.getField(nav, "ticking"));
        nav.stopMotion();
        ViewGroup parent = (ViewGroup) nav.getParent();
        parent.removeView(nav);
        assertNull("detach removes the source pre-draw watcher", ReflectionHelpers.getField(nav, "watchedObserver"));
        assertFalse(ReflectionHelpers.getField(nav, "ticking"));
    }

    @Test public void reducedMotionSnapsStateWithoutSchedulingAFrame() {
        MainActivity activity = activity();
        GlassNavPill nav = activity.findViewById(R.id.bottom_navigation);
        durationScale(0f);
        nav.setSelectedItemId(R.id.nav_help);
        nav.setMinimized(true);
        assertEquals(R.id.nav_help, nav.getSelectedItemId());
        assertTrue(nav.isMinimized());
        assertFalse(ReflectionHelpers.getField(nav, "ticking"));
        nav.setMinimized(false);
        assertFalse(nav.isMinimized());
        assertFalse(ReflectionHelpers.getField(nav, "ticking"));
    }

    @Test public void touchExplorationBlocksAutomaticCompactionButLeavesTabsNavigable() {
        MainActivity activity = activity();
        GlassNavPill nav = activity.findViewById(R.id.bottom_navigation);
        AccessibilityManager manager = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        assertNotNull(manager);
        shadowOf(manager).setEnabled(true);
        shadowOf(manager).setTouchExplorationEnabled(true);
        assertTrue(manager.isTouchExplorationEnabled());
        nav.onPageScroll(200, 100);
        assertFalse("TalkBack mode must retain the full-size tab bar", nav.isMinimized());
        nav.setSelectedItemId(R.id.nav_help);
        assertEquals(R.id.nav_help, nav.getSelectedItemId());
    }

    @Test public void springTravelDoesNotRequestLayoutMeasureOrLayout() {
        MainActivity activity = activity();
        FrameLayout root = activity.findViewById(R.id.main);
        InstrumentedPill pill = new InstrumentedPill(activity);
        root.addView(pill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                GlassNavPill.heightFor(activity)));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));
        pill.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(270, View.MeasureSpec.EXACTLY));
        pill.layout(0, 0, 1080, 270);
        pill.resetCounters();
        pill.setOnItemSelectedListener(null);
        pill.setSelectedItemId(R.id.nav_features);
        pill.setSelectedItemId(R.id.nav_logs);
        pill.setSelectedItemId(R.id.nav_help);
        pill.setSelectedItemId(R.id.nav_home);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2300));
        assertEquals(0, pill.requestCount);
        assertEquals(0, pill.measureCount);
        assertEquals(0, pill.layoutCount);
        assertFalse(ReflectionHelpers.getField(pill, "ticking"));
        root.removeView(pill);
    }

    private static final class InstrumentedPill extends GlassNavPill {
        int requestCount;
        int measureCount;
        int layoutCount;
        InstrumentedPill(Context context) { super(context); }
        @Override public void requestLayout() { requestCount++; super.requestLayout(); }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            measureCount++;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            layoutCount++;
            super.onLayout(changed, left, top, right, bottom);
        }
        void resetCounters() { requestCount = measureCount = layoutCount = 0; }
    }

    private static void durationScale(float scale) {
        ReflectionHelpers.callStaticMethod(ValueAnimator.class, "setDurationScale",
                ReflectionHelpers.ClassParameter.from(float.class, scale));
    }
}
