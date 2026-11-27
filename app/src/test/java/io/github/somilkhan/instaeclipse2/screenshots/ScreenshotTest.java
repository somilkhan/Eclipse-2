package io.github.somilkhan.instaeclipse2.screenshots;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;

import androidx.fragment.app.Fragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import io.github.somilkhan.instaeclipse2.MainActivity;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.fragments.FeaturesFragment;
import io.github.somilkhan.instaeclipse2.fragments.HelpFragment;
import io.github.somilkhan.instaeclipse2.fragments.HomeFragment;
import io.github.somilkhan.instaeclipse2.fragments.LoggingFragment;

/**
 * Renders each companion screen with Robolectric native graphics (hardware PixelCopy, so
 * RenderNode / RenderEffect paths run) in light and dark. Always asserts the render is non-blank;
 * writes PNGs to docs/screenshots/ only when run with -Pie.screenshots=true.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
public class ScreenshotTest {


    @Test public void homeLight() { render("home_light", null, false); }
    @Test @Config(qualifiers = "+night") public void homeDark() { render("home_dark", null, true); }
    @Test public void homeScrolledLight() { render("home_scrolled_light", null, false, 900); }
    @Test @Config(qualifiers = "+night") public void homeScrolledDark() { render("home_scrolled_dark", null, true, 900); }
    @Test public void featuresLight() { render("features_light", new FeaturesFragment(), false); }
    @Test @Config(qualifiers = "+night") public void featuresDark() { render("features_dark", new FeaturesFragment(), true); }
    @Test public void featuresScrolledLight() { render("features_scrolled_light", new FeaturesFragment(), false, 700); }
    @Test @Config(qualifiers = "+night") public void featuresScrolledDark() { render("features_scrolled_dark", new FeaturesFragment(), true, 700); }
    @Test public void logsLight() { render("logs_light", new LoggingFragment(), false); }
    @Test @Config(qualifiers = "+night") public void logsDark() { render("logs_dark", new LoggingFragment(), true); }
    @Test public void helpLight() { render("help_light", new HelpFragment(), false); }
    @Test @Config(qualifiers = "+night") public void helpDark() { render("help_dark", new HelpFragment(), true); }

    @org.junit.Before
    public void setUp() {
        // Infinite animations (auto-scrolling carousels, indeterminate progress) would otherwise
        // keep the paused looper busy forever: give frames a real 16 ms cadence.
        io.github.somilkhan.instaeclipse2.fragments.HomeFragment.autoScrollEnabled = false;
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
    }

    private void render(String name, Fragment fragment, boolean dark) {
        render(name, fragment, dark, 0);
    }

    private void render(String name, Fragment fragment, boolean dark, int scrollDp) {
        ScreenshotSupport.installFakeInstagram();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        if (fragment != null) {
            int tab = fragment instanceof FeaturesFragment ? R.id.nav_features
                    : fragment instanceof LoggingFragment ? R.id.nav_logs
                    : fragment instanceof HelpFragment ? R.id.nav_help : R.id.nav_home;
            ScreenshotSupport.selectTab(activity, tab, fragment);
        }
        idle();
        ScreenshotSupport.applyPhoneInsets(activity);
        idle();
        if (scrollDp > 0) {
            View container = activity.findViewById(R.id.fragment_container);
            View scroller = container.findViewWithTag("ie_scroll");
            if (scroller == null) scroller = container.findViewWithTag("ie_scroll_list");
            int px = Math.round(scrollDp * activity.getResources().getDisplayMetrics().density);
            if (scroller != null) scroller.scrollBy(0, px);
            idle();
        }
        View decor = activity.getWindow().getDecorView();
        Bitmap bmp = capture(activity, decor);
        assertTrue("render is blank: " + name, ScreenshotSupport.hasDetail(bmp));
        String dir = System.getProperty("instaeclipse.screenshotDir");
        if (dir != null) {
            File out = new File(dir, name + ".png");
            out.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        controller.pause().stop().destroy();
    }

    private static void idle() {
        for (int i = 0; i < 6; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));
        }
    }

    private static Bitmap capture(MainActivity activity, View decor) {
        Bitmap bmp = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        final int[] result = {-1};
        try {
            PixelCopy.request(activity.getWindow(), bmp, r -> result[0] = r, new Handler(Looper.getMainLooper()));
            shadowOf(Looper.getMainLooper()).idle();
        } catch (Throwable ignored) { }
        if (result[0] != PixelCopy.SUCCESS) {
            android.graphics.Canvas c = new android.graphics.Canvas(bmp);
            decor.draw(c);
        }
        return bmp;
    }
}
