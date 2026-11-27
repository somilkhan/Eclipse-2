package io.github.somilkhan.instaeclipse2.screenshots;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.ArrayList;
import java.util.List;

import io.github.somilkhan.instaeclipse2.MainActivity;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.fragments.FeaturesFragment;
import io.github.somilkhan.instaeclipse2.fragments.HelpFragment;
import io.github.somilkhan.instaeclipse2.fragments.LoggingFragment;
import io.github.somilkhan.instaeclipse2.mods.location.LocationPickerActivity;
import io.github.somilkhan.instaeclipse2.ui.theme.ThemeCustomizerActivity;

/**
 * Edge-to-edge regression guard (device bug: Logs drawn under the status bar / camera cutout).
 * Feeds a tall status bar plus a deeper display cutout and a gesture bar, then checks that at
 * rest no visible text/control of any screen starts above the top inset, and that no visible
 * control of a non-scrolling panel ends behind the floating nav pill.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
public class InsetOverlapTest {

    private static final int STATUS_DP = 32;
    private static final int CUTOUT_DP = 52; // punch-hole / notch deeper than the status bar
    private static final int NAV_DP = 24;

    @Before
    public void setUp() {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
        io.github.somilkhan.instaeclipse2.MainActivity.prewarmTabs = false;
        io.github.somilkhan.instaeclipse2.fragments.HomeFragment.autoScrollEnabled = false;
    }

    @Test public void home() { tab(0, null); }
    @Test public void features() { tab(R.id.nav_features, new FeaturesFragment()); }
    @Test public void logs() { tab(R.id.nav_logs, new LoggingFragment()); }
    @Test public void help() { tab(R.id.nav_help, new HelpFragment()); }
    @Test @Config(qualifiers = "ar-w393dp-h852dp-xxhdpi") public void logsRtl() { tab(R.id.nav_logs, new LoggingFragment()); }

    @Test public void themeCustomizer() {
        ActivityController<ThemeCustomizerActivity> c = Robolectric.buildActivity(ThemeCustomizerActivity.class).setup();
        check(c.get(), c.get().findViewById(android.R.id.content), null);
        c.pause().stop().destroy();
    }

    @Test public void locationPicker() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                LocationPickerActivity.class);
        ActivityController<LocationPickerActivity> c = Robolectric.buildActivity(LocationPickerActivity.class, i).setup();
        check(c.get(), c.get().findViewById(android.R.id.content), null);
        c.pause().stop().destroy();
    }

    private void tab(int navId, Fragment f) {
        ScreenshotSupport.installFakeInstagram();
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        if (navId != 0) ScreenshotSupport.selectTab(a, navId, f);
        check(a, a.findViewById(R.id.fragment_container), a.findViewById(R.id.bottom_navigation));
        c.pause().stop().destroy();
    }

    private void check(Activity a, View scope, View pill) {
        idle();
        float d = a.getResources().getDisplayMetrics().density;
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, Math.round(STATUS_DP * d), 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, Math.round(CUTOUT_DP * d), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, Math.round(NAV_DP * d)))
                .build();
        ViewGroup content = a.findViewById(android.R.id.content);
        ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets);
        idle();

        int top = Math.round(CUTOUT_DP * d);
        int pillTop = Integer.MAX_VALUE;
        if (pill != null) {
            Rect r = new Rect();
            pill.getGlobalVisibleRect(r);
            pillTop = r.top;
        }
        List<String> bad = new ArrayList<>();
        collect(scope, top, pillTop, bad, false);
        assertTrue(a.getClass().getSimpleName() + ": views overlap system bars / pill: " + bad, bad.isEmpty());
    }

    /** Leaf views (text, buttons, chips) that are visible on screen. */
    private static void collect(View v, int top, int pillTop, List<String> bad, boolean inScroller) {
        if (v.getVisibility() != View.VISIBLE || v.getAlpha() == 0f) return;
        if (v instanceof org.osmdroid.views.MapView) return; // the map is meant to run under the bars
        boolean scroller = inScroller || v instanceof androidx.core.widget.NestedScrollView
                || v instanceof android.widget.ScrollView || v instanceof androidx.recyclerview.widget.RecyclerView;
        if (v instanceof ViewGroup && !(v instanceof com.google.android.material.chip.Chip)
                && !(v instanceof android.widget.Button)) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), top, pillTop, bad, scroller);
            return;
        }
        if (!(v instanceof android.widget.TextView) && !(v instanceof android.widget.ImageView)
                && !(v instanceof android.widget.CompoundButton)) return;
        Rect r = new Rect();
        if (!v.getGlobalVisibleRect(r) || r.height() == 0) return;
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        String name = describe(v);
        if (loc[1] < top) bad.add(name + " top=" + loc[1] + " < " + top);
        // Fixed (non-scrolling) controls must end above the floating pill; scrolling content
        // just needs bottom padding, which the screenshot tests cover.
        if (!inScroller && pillTop != Integer.MAX_VALUE && loc[1] + v.getHeight() > pillTop) {
            bad.add(name + " bottom=" + (loc[1] + v.getHeight()) + " > pill " + pillTop);
        }
    }

    private static String describe(View v) {
        String id = v.getId() != View.NO_ID ? v.getResources().getResourceEntryName(v.getId()) : v.getClass().getSimpleName();
        if (v instanceof android.widget.TextView) id += "('" + ((android.widget.TextView) v).getText() + "')";
        return id;
    }

    private static void idle() {
        for (int i = 0; i < 6; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));
        }
    }
}
