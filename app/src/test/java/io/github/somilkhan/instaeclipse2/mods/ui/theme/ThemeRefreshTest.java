package io.github.somilkhan.instaeclipse2.mods.ui.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ThemeRefreshTest {

    private final AtomicInteger runs = new AtomicInteger();
    private Runnable saved;

    @Before
    public void setUp() {
        saved = ThemeRefresh.refresh;
        ThemeRefresh.refresh = runs::incrementAndGet;
    }

    @After
    public void tearDown() {
        ThemeRefresh.refresh = saved;
    }

    @Test
    public void onlyThemeKeysAreThemeKeys() {
        assertTrue(ThemeRefresh.isThemeKey("customThemeEnabled"));
        assertTrue(ThemeRefresh.isThemeKey("themePresetId"));
        assertTrue(ThemeRefresh.isThemeKey("themeLightPresetId"));
        assertTrue(ThemeRefresh.isThemeKey("themePaletteJson"));
        assertFalse(ThemeRefresh.isThemeKey("enableGhostMode"));
        assertFalse(ThemeRefresh.isThemeKey("customFontEnabled"));
        assertFalse(ThemeRefresh.isThemeKey(null));
    }

    @Test
    public void nonThemeKeyDoesNotRefresh() {
        ThemeRefresh.onPrefChanged("enableGhostMode");
        ThemeRefresh.onPrefChanged("disableStories");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(0, runs.get());
    }

    @Test
    public void burstOfThemeKeysRefreshesOnce() {
        ThemeRefresh.onPrefChanged("customThemeEnabled");
        ThemeRefresh.onPrefChanged("enableGhostMode");
        ThemeRefresh.onPrefChanged("themePresetId");
        ThemeRefresh.onPrefChanged("themePaletteJson");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ThemeRefresh.DEBOUNCE_MS - 50));
        assertEquals(0, runs.get());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(1, runs.get());

        ThemeRefresh.onPrefChanged("themeLightPresetId");
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(2, runs.get());
    }
}
