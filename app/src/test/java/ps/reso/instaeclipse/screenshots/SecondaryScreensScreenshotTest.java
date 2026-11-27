package ps.reso.instaeclipse.screenshots;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;

import ps.reso.instaeclipse.MainActivity;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.fragments.FeaturesFragment;
import ps.reso.instaeclipse.fragments.LoggingFragment;
import ps.reso.instaeclipse.mods.location.LocationPickerActivity;
import ps.reso.instaeclipse.ui.theme.AdvancedColorPickerDialog;
import ps.reso.instaeclipse.ui.theme.ThemeCustomizerActivity;

/**
 * Screens outside the four tabs (theme customizer, colour picker, location picker, Home dialogs)
 * plus RTL (Arabic) renders of the tabs. Same rules as {@link ScreenshotTest}: always asserts the
 * render is non-blank, writes PNGs only with -Pie.screenshots=true.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
public class SecondaryScreensScreenshotTest {

    @Before
    public void setUp() {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
    }

    // ---- theme customizer ----
    @Test public void themeLight() { theme("theme_customizer_light", false, 0); }
    @Test @Config(qualifiers = "+night") public void themeDark() { theme("theme_customizer_dark", false, 0); }
    @Test @Config(qualifiers = "+night") public void themeDarkScrolled() { theme("theme_customizer_scrolled_dark", false, 700); }
    @Test public void colorPickerLight() { theme("color_picker_light", true, 0); }
    @Test @Config(qualifiers = "+night") public void colorPickerDark() { theme("color_picker_dark", true, 0); }

    // ---- location picker ----
    @Test public void locationLight() { location("location_picker_light"); }
    @Test @Config(qualifiers = "+night") public void locationDark() { location("location_picker_dark"); }

    // ---- Home dialog ----
    @Test public void shortcutsDialogLight() { shortcutsDialog("dialog_shortcuts_light"); }
    @Test @Config(qualifiers = "+night") public void shortcutsDialogDark() { shortcutsDialog("dialog_shortcuts_dark"); }

    // ---- RTL ----
    @Test @Config(qualifiers = "ar-w393dp-h852dp-xxhdpi") public void homeRtl() { tab("home_rtl_light", 0); }
    @Test @Config(qualifiers = "ar-w393dp-h852dp-night-xxhdpi") public void featuresRtl() { tab("features_rtl_dark", R.id.nav_features); }
    @Test @Config(qualifiers = "iw-w393dp-h852dp-xxhdpi") public void logsRtl() { tab("logs_rtl_light", R.id.nav_logs); }
    @Test @Config(qualifiers = "ar-w393dp-h852dp-xxhdpi") public void themeRtl() { theme("theme_customizer_rtl_light", false, 0); }

    private void theme(String name, boolean picker, int scrollDp) {
        ActivityController<ThemeCustomizerActivity> c =
                Robolectric.buildActivity(ThemeCustomizerActivity.class).setup();
        ThemeCustomizerActivity a = c.get();
        idle();
        applyInsets(a);
        idle();
        if (scrollDp > 0) {
            View scroller = findScroller(a.getWindow().getDecorView());
            if (scroller != null) scroller.scrollBy(0, dp(a, scrollDp));
            idle();
        }
        Dialog dialog = null;
        if (picker) {
            AdvancedColorPickerDialog.newInstance(a.getString(R.string.theme_title), 0xFF5B5BD6)
                    .show(a.getSupportFragmentManager(), "colorPicker");
            a.getSupportFragmentManager().executePendingTransactions();
            idle();
            dialog = ShadowDialog.getLatestDialog();
        }
        save(name, a, dialog);
        c.pause().stop().destroy();
    }

    private void location(String name) {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                LocationPickerActivity.class)
                .putExtra(LocationPickerActivity.EXTRA_LAT, 48.8584)
                .putExtra(LocationPickerActivity.EXTRA_LNG, 2.2945);
        ActivityController<LocationPickerActivity> c = Robolectric.buildActivity(LocationPickerActivity.class, i).setup();
        LocationPickerActivity a = c.get();
        idle();
        applyInsets(a);
        idle();
        // Offline: no tiles and no reverse geocoding. Show a realistic label.
        android.widget.TextView place = a.findViewById(R.id.place_name);
        place.setText("Champ de Mars, Paris");
        idle();
        save(name, a, null);
        c.pause().stop().destroy();
    }

    private void shortcutsDialog(String name) {
        ScreenshotSupport.installFakeInstagram();
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        idle();
        ScreenshotSupport.applyPhoneInsets(a);
        idle();
        a.findViewById(R.id.home_shortcuts_edit).performClick();
        idle();
        save(name, a, ShadowDialog.getLatestDialog());
        c.pause().stop().destroy();
    }

    private void tab(String name, int navId) {
        ScreenshotSupport.installFakeInstagram();
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        if (navId != 0) {
            ScreenshotSupport.selectTab(a, navId, navId == R.id.nav_logs ? new LoggingFragment() : new FeaturesFragment());
        }
        idle();
        ScreenshotSupport.applyPhoneInsets(a);
        idle();
        save(name, a, null);
        c.pause().stop().destroy();
    }

    // ---------------------------------------------------------------- helpers

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static void applyInsets(Activity a) {
        WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, dp(a, 36), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, dp(a, 20)))
                .build();
        ViewGroup content = a.findViewById(android.R.id.content);
        View root = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        ViewCompat.dispatchApplyWindowInsets(root, insets);
    }

    private static View findScroller(View v) {
        if (v instanceof androidx.core.widget.NestedScrollView || v instanceof android.widget.ScrollView) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findScroller(g.getChildAt(i));
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void idle() {
        for (int i = 0; i < 6; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));
        }
    }

    private static void save(String name, FragmentActivity a, Dialog dialog) {
        View decor = a.getWindow().getDecorView();
        Bitmap bmp = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        decor.draw(canvas);
        if (dialog != null) {
            // Compose the dialog window over a dimmed activity, centred like the window manager does.
            canvas.drawColor(Color.argb(Math.round(0.32f * 255), 0, 0, 0));
            View d = dialog.getWindow().getDecorView();
            int maxW = decor.getWidth();
            if (d.getWidth() == 0 || d.getWidth() > maxW) {
                d.measure(View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(decor.getHeight(), View.MeasureSpec.AT_MOST));
                d.layout(0, 0, d.getMeasuredWidth(), d.getMeasuredHeight());
            }
            canvas.save();
            canvas.translate((decor.getWidth() - d.getWidth()) / 2f, (decor.getHeight() - d.getHeight()) / 2f);
            d.draw(canvas);
            canvas.restore();
        }
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
    }
}
