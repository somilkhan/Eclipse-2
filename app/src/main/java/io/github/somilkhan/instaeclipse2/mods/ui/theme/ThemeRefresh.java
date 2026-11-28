package io.github.somilkhan.instaeclipse2.mods.ui.theme;

import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Applies a theme change coming from a settings sync. Only the theme's own keys trigger it, and a
 * burst of changes (the companion sends one broadcast per staged key) is coalesced into a single
 * {@link IgThemeHook#refreshCurrentActivity()}: that recreates Instagram's current activity, so
 * running it for unrelated keys or once per key lost the user's screen state.
 */
public final class ThemeRefresh {

    /** Preference keys that change the custom theme's output. */
    public static final Set<String> THEME_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "customThemeEnabled", "themePresetId", "themeLightPresetId", "themePaletteJson")));

    static final long DEBOUNCE_MS = 300;

    private static Handler handler;
    /** Replaced in tests. */
    static Runnable refresh = IgThemeHook::refreshCurrentActivity;
    private static final Runnable RUN = () -> {
        try {
            refresh.run();
        } catch (Throwable ignored) {
        }
    };

    private ThemeRefresh() {}

    public static boolean isThemeKey(String key) {
        return key != null && THEME_KEYS.contains(key);
    }

    /** Called for every synced preference; refreshes (debounced) only for theme keys. */
    public static void onPrefChanged(String key) {
        if (!isThemeKey(key)) return;
        IgThemeEngine.invalidate(); // cheap: the palette is rebuilt lazily
        request();
    }

    /** Schedules one refresh {@link #DEBOUNCE_MS} after the last request. */
    public static synchronized void request() {
        if (handler == null) handler = new Handler(Looper.getMainLooper());
        handler.removeCallbacks(RUN);
        handler.postDelayed(RUN, DEBOUNCE_MS);
    }
}
