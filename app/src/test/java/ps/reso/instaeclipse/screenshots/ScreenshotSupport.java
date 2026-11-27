package ps.reso.instaeclipse.screenshots;

import android.graphics.Bitmap;

import androidx.fragment.app.Fragment;

import ps.reso.instaeclipse.MainActivity;
import ps.reso.instaeclipse.R;

final class ScreenshotSupport {
    private ScreenshotSupport() { }

    /** Selects the tab through the floating pill exactly like a tap (fragment is created by the activity). */
    static void selectTab(MainActivity activity, int tabId, Fragment fragment) {
        activity.selectTab(tabId);
        activity.getSupportFragmentManager().executePendingTransactions();
    }

    /**
     * Robolectric has no system bars: feed typical phone insets (status bar + gesture nav) so the
     * edge-to-edge layout is rendered exactly as on a device.
     */
    static void applyPhoneInsets(MainActivity activity) {
        float d = activity.getResources().getDisplayMetrics().density;
        androidx.core.view.WindowInsetsCompat insets = new androidx.core.view.WindowInsetsCompat.Builder()
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars(),
                        androidx.core.graphics.Insets.of(0, Math.round(36 * d), 0, 0))
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars(),
                        androidx.core.graphics.Insets.of(0, 0, 0, Math.round(20 * d)))
                .build();
        androidx.core.view.ViewCompat.dispatchApplyWindowInsets(activity.findViewById(R.id.main), insets);
    }

    /** Pretends the tested Instagram build is installed so Home shows its real "installed" state. */
    static void installFakeInstagram() {
        android.content.pm.PackageInfo pi = new android.content.pm.PackageInfo();
        pi.packageName = "com.instagram.android";
        pi.versionName = "457.0.0.55.73";
        pi.applicationInfo = new android.content.pm.ApplicationInfo();
        pi.applicationInfo.packageName = pi.packageName;
        pi.applicationInfo.name = "Instagram";
        org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider
                .<android.app.Application>getApplicationContext().getPackageManager()).installPackage(pi);
    }

    /** True when the bitmap has more than a handful of distinct colours (i.e. not blank). */
    static boolean hasDetail(Bitmap bmp) {
        java.util.HashSet<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < bmp.getHeight(); y += 7) {
            for (int x = 0; x < bmp.getWidth(); x += 7) {
                colors.add(bmp.getPixel(x, y));
                if (colors.size() > 32) return true;
            }
        }
        return false;
    }
}
