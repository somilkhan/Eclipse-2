package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Decides whether the Liquid-Glass-Android library (real lens refraction, API 33+) may be used,
 * or whether the in-house {@link BackdropBlurLayout} path must be used instead.
 *
 * <p>The library is only used when <b>all</b> of these hold:</p>
 * <ul>
 *   <li>API 33+ (its AGSL lens pipeline; below that it would fall to its CPU pipeline);</li>
 *   <li>its native helper {@code libnativegauss.so} loads (it only ships arm64-v8a / armeabi-v7a,
 *       and the library itself only catches {@code Exception}, so an {@code UnsatisfiedLinkError}
 *       inside it would crash the app — we probe it first);</li>
 *   <li>no battery saver and no "high contrast text" (our fallback is calmer / more opaque);</li>
 *   <li>not disabled for tests ({@code -Dinstaeclipse.softBlur=true} / {@code instaeclipse.noLiquid}).</li>
 * </ul>
 * Any runtime failure inside a library view calls {@link #markBroken()}, after which every glass
 * surface stays on the fallback for the rest of the process.
 */
public final class LiquidSupport {
    private static final String TAG = "IE-Glass";
    private static Boolean libOk;
    private static volatile boolean broken;

    private LiquidSupport() { }

    public static boolean available(Context ctx) {
        if (broken) return false;
        if (Build.VERSION.SDK_INT < 33) return false;
        if (Boolean.getBoolean("instaeclipse.softBlur") || Boolean.getBoolean("instaeclipse.noLiquid")) return false;
        if (powerSave(ctx) || highContrast(ctx)) return false;
        return libraryLoads();
    }

    private static synchronized boolean libraryLoads() {
        if (libOk == null) {
            boolean ok;
            try {
                System.loadLibrary("nativegauss");
                Class.forName("com.example.liquidglass.LiquidGlassView");
                ok = true;
            } catch (Throwable t) {
                Log.w(TAG, "liquidglass unavailable, using fallback glass: " + t);
                ok = false;
            }
            libOk = ok;
        }
        return libOk;
    }

    public static void markBroken(Throwable t) {
        if (!broken) Log.w(TAG, "liquidglass failed at runtime, switching to fallback glass", t);
        broken = true;
    }

    public static boolean isBroken() { return broken; }

    public static boolean powerSave(Context ctx) {
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isPowerSaveMode();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Android's closest equivalent to iOS "Reduce Transparency" / "Increase Contrast". */
    public static boolean highContrast(Context ctx) {
        try {
            return Settings.Secure.getInt(ctx.getContentResolver(), "high_text_contrast_enabled", 0) == 1;
        } catch (Throwable t) {
            return false;
        }
    }
}
