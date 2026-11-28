package io.github.somilkhan.instaeclipse2.screenshots;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.somilkhan.instaeclipse2.MainActivity;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.fragments.HomeFragment;
import io.github.somilkhan.instaeclipse2.ui.glass.GlassNavPill;
import io.github.somilkhan.instaeclipse2.ui.glass.TabReveal;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.toast.CustomToast;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
public class HyperSpringNavigationTest {
    private ActivityController<MainActivity> controller;

    @Before public void setup() {
        MainActivity.prewarmTabs = false;
        HomeFragment.autoScrollEnabled = false;
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
        durationScale(1f);
        ScreenshotSupport.installFakeInstagram();
    }

    @After public void teardown() {
        if (controller != null) controller.pause().stop().destroy();
        durationScale(1f);
        MainActivity.prewarmTabs = true;
        HomeFragment.autoScrollEnabled = true;
    }

    private MainActivity activity() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1200));
        ScreenshotSupport.applyPhoneInsets(controller.get());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        return controller.get();
    }

    private static void durationScale(float scale) {
        ReflectionHelpers.callStaticMethod(ValueAnimator.class, "setDurationScale",
                ReflectionHelpers.ClassParameter.from(float.class, scale));
    }

    @Test public void rapidSwitchAndReselectKeepOneResumedTabAndReleaseSnapshots() {
        MainActivity a = activity();
        ViewGroup stage = a.findViewById(R.id.content_stage);
        int baseStageChildren = stage.getChildCount();
        a.selectTab(R.id.nav_features);
        Fragment features = a.getSupportFragmentManager().findFragmentByTag("ie_tab_" + R.id.nav_features);
        assertNotNull(features);
        a.selectTab(R.id.nav_logs);
        a.selectTab(R.id.nav_features);
        a.selectTab(R.id.nav_features);
        assertSame(features, a.getSupportFragmentManager().findFragmentByTag("ie_tab_" + R.id.nav_features));
        // With the paused Choreographer, advancing in frame batches also starts the queued
        // animator frame before advancing its duration (same cadence as ScreenshotTest).
        for (int i = 0; i < 6; i++) shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
        assertEquals(baseStageChildren, stage.getChildCount());
        ViewGroup container = a.findViewById(R.id.fragment_container);
        int resumed = 0, shown = 0;
        for (Fragment f : a.getSupportFragmentManager().getFragments()) {
            if (f.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) resumed++;
            if (!f.isHidden()) shown++;
            assertEquals(1f, f.requireView().getScaleX(), 0.001f);
            assertEquals(1f, f.requireView().getAlpha(), 0.001f);
        }
        assertEquals(1, resumed);
        assertEquals(1, shown);
        assertEquals(a.getSupportFragmentManager().getFragments().size(), container.getChildCount());
    }

    @Test public void stopDuringRevealRemovesRingAndSnapshot() {
        MainActivity a = activity();
        ViewGroup stage = a.findViewById(R.id.content_stage);
        int before = stage.getChildCount();
        a.selectTab(R.id.nav_help);
        controller.pause().stop();
        assertEquals(before, stage.getChildCount());
        ViewGroup container = a.findViewById(R.id.fragment_container);
        assertEquals(a.getSupportFragmentManager().getFragments().size(), container.getChildCount());
        controller.destroy();
        controller = null;
    }

    @Test public void reducedMotionSkipsWaveAndCompactKeepsEveryAccessibleTarget() {
        MainActivity a = activity();
        durationScale(0f);
        ViewGroup stage = a.findViewById(R.id.content_stage);
        int before = stage.getChildCount();
        a.selectTab(R.id.nav_help);
        assertEquals(before, stage.getChildCount());
        GlassNavPill nav = a.findViewById(R.id.bottom_navigation);
        nav.setMinimized(true);
        assertTrue(nav.isMinimized());
        assertEquals(0.86f, nav.getScaleX(), 0.001f);
        for (int id : new int[]{R.id.nav_home, R.id.nav_features, R.id.nav_logs, R.id.nav_help}) {
            View tab = nav.findViewById(id);
            assertNotNull(tab);
            assertEquals(View.VISIBLE, tab.getVisibility());
            assertEquals(1f, tab.getAlpha(), 0.001f);
            assertTrue(tab.isClickable());
            assertTrue(tab.isFocusable());
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, tab.getImportantForAccessibility());
            assertTrue(tab.getContentDescription().length() > 0);
            assertTrue(tab.getHeight() * nav.getScaleY() / a.getResources().getDisplayMetrics().density >= 48f);
        }
        nav.setMinimized(false);
        assertEquals(1f, nav.getScaleX(), 0.001f);
    }

    @Test public void rapidTabSelectionCleansIconTransformsAndExposesSelectedSemantics() {
        MainActivity a = activity();
        GlassNavPill nav = a.findViewById(R.id.bottom_navigation);
        View home = nav.findViewById(R.id.nav_home);
        View features = nav.findViewById(R.id.nav_features);
        assertNotNull(home);
        assertNotNull(features);

        a.selectTab(R.id.nav_features);
        a.selectTab(R.id.nav_logs);
        a.selectTab(R.id.nav_home);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));

        for (int id : new int[]{R.id.nav_home, R.id.nav_features, R.id.nav_logs, R.id.nav_help}) {
            View tab = nav.findViewById(id);
            View icon = ((ViewGroup) tab).getChildAt(0);
            assertEquals("icon scale must settle after interrupted switching", 1f,
                    icon.getScaleX(), 0.02f);
            assertEquals(1f, icon.getScaleY(), 0.02f);
            assertTrue(tab.createAccessibilityNodeInfo().isCheckable());
        }
        AccessibilityNodeInfo selected = home.createAccessibilityNodeInfo();
        assertTrue(selected.isSelected());
        assertFalse(features.createAccessibilityNodeInfo().isSelected());
    }

    @Test public void savedSelectionRestoresMatchingFragmentAndPill() {
        MainActivity a = activity();
        a.selectTab(R.id.nav_logs);
        Bundle saved = new Bundle();
        controller.saveInstanceState(saved).pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class)
                .create(saved).start().restoreInstanceState(saved).resume().visible();
        GlassNavPill nav = controller.get().findViewById(R.id.bottom_navigation);
        assertEquals(R.id.nav_logs, nav.getSelectedItemId());
        Fragment logs = controller.get().getSupportFragmentManager().findFragmentByTag("ie_tab_" + R.id.nav_logs);
        assertNotNull(logs);
        assertFalse(logs.isHidden());
        assertEquals(Lifecycle.State.RESUMED, logs.getLifecycle().getCurrentState());
    }

    @Test public void companionUsesCurrentReferencePaletteEvenWithLightDeviceQualifier() {
        MainActivity a = activity();
        assertEquals(Color.BLACK, a.getResources().getColor(R.color.ie_mesh_base, a.getTheme()));
        assertEquals(0xFFFF5F45, a.getResources().getColor(R.color.ie_primary, a.getTheme()));
        assertEquals(0xFF0E0E10, a.getResources().getColor(R.color.ie_glass_fill, a.getTheme()));
    }

    @Test public void featureDetailsOpensEclipseBottomSheetWithClearMeaningsAndFeatureRows() {
        MainActivity a = activity();
        Map<String, FeatureStatusTracker.HealthState> status = new LinkedHashMap<>();
        status.put("Runtime feature", FeatureStatusTracker.HealthState.RUNTIME_OBSERVED);
        status.put("Attached feature", FeatureStatusTracker.HealthState.INSTALLED);
        status.put("Unchecked feature", FeatureStatusTracker.HealthState.NOT_EXERCISED);
        status.put("Broken feature", FeatureStatusTracker.HealthState.FAILED);
        CustomToast.showFeatureSummary(a, "Eclipse loaded", status);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        View content = a.findViewById(android.R.id.content);
        View details = findText(content, "Details");
        assertNotNull("compact banner should have a Details action", details);
        details.performClick();

        android.app.Dialog dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        View sheet = dialog.getWindow().getDecorView().findViewWithTag(
                "io.github.somilkhan.instaeclipse2.feature_health_sheet");
        assertNotNull("details must use Eclipse's own sheet, not a framework alert dialog", sheet);
        assertNotNull(findText(sheet, "Working"));
        assertNotNull(findText(sheet, "Hook attached"));
        assertNotNull(findText(sheet, "Not checked"));
        assertNotNull(findText(sheet, "Failed"));
        assertNotNull(findText(sheet, "Runtime feature"));
        assertNotNull(findText(sheet, "Attached feature"));
        assertNotNull(findText(sheet, "Unchecked feature"));
        assertNotNull(findText(sheet, "Broken feature"));
        dialog.dismiss();
    }

    private static View findText(View root, String expected) {
        if (root instanceof TextView && expected.contentEquals(((TextView) root).getText())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = findText(group.getChildAt(i), expected);
                if (result != null) return result;
            }
        }
        return null;
    }

    @Test public void revealFinishRestoresOutlineAndPivotAndRunsCleanupOnce() {
        FrameLayout root = root();
        View page = root.getChildAt(0);
        View old = new View(root.getContext());
        root.addView(old, new FrameLayout.LayoutParams(-1, -1));
        ViewOutlineProvider outline = page.getOutlineProvider();
        page.setPivotX(7f);
        page.setPivotY(11f);
        old.setPivotX(13f);
        old.setPivotY(17f);
        int[] done = {0};
        TabReveal reveal = new TabReveal();
        reveal.play(root, page, Collections.singletonList(old), page, 180f, 760f, () -> done[0]++);
        assertEquals(3, root.getChildCount());
        reveal.finish();
        reveal.finish();
        assertEquals(1, done[0]);
        assertEquals(2, root.getChildCount());
        assertSame(outline, page.getOutlineProvider());
        assertFalse(page.getClipToOutline());
        assertEquals(7f, page.getPivotX(), 0f);
        assertEquals(11f, page.getPivotY(), 0f);
        assertEquals(13f, old.getPivotX(), 0f);
        assertEquals(17f, old.getPivotY(), 0f);
        assertEquals(1f, page.getScaleX(), 0f);
    }

    @Test public void revealOwnershipIsNotSharedBetweenActivities() {
        FrameLayout one = root(), two = root();
        TabReveal first = new TabReveal(), second = new TabReveal();
        first.play(one, one.getChildAt(0), Collections.emptyList(), one.getChildAt(0), 90f, 760f, () -> {});
        second.play(two, two.getChildAt(0), Collections.emptyList(), two.getChildAt(0), 180f, 760f, () -> {});
        assertEquals(2, one.getChildCount());
        assertEquals(2, two.getChildCount());
        second.finish();
        assertEquals(2, one.getChildCount());
        first.finish();
        assertEquals(1, one.getChildCount());
        assertEquals(1, two.getChildCount());
    }

    private static FrameLayout root() {
        FrameLayout root = new FrameLayout(ApplicationProvider.getApplicationContext());
        root.addView(new View(root.getContext()), new FrameLayout.LayoutParams(-1, -1));
        root.measure(View.MeasureSpec.makeMeasureSpec(393, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(852, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 393, 852);
        return root;
    }
}
