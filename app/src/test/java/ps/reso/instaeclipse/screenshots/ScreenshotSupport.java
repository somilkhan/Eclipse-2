package ps.reso.instaeclipse.screenshots;

import android.graphics.Bitmap;

import androidx.fragment.app.Fragment;

import ps.reso.instaeclipse.MainActivity;
import ps.reso.instaeclipse.R;

final class ScreenshotSupport {
    private ScreenshotSupport() { }

    static void selectTab(MainActivity activity, int tabId, Fragment fragment) {
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commitNow();
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
