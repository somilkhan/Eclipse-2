package io.github.somilkhan.instaeclipse2.utils.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RestartRequiredTest {

    private static final String NOTE = "Restart Instagram to apply";

    @Test
    public void installTimeFeaturesAreListed() {
        for (String k : new String[] {"customThemeEnabled", "customFontEnabled", "customEmojiEnabled",
                "disableVideoAutoPlay", "customStoryRingSize"}) {
            assertTrue(k, RestartRequired.isRestartRequired(k));
        }
        assertFalse(RestartRequired.isRestartRequired("enableGhostMode"));
        assertFalse(RestartRequired.isRestartRequired(null));
    }

    @Test
    public void noteIsAppendedOnlyForRestartKeys() {
        assertEquals("Custom font (" + NOTE + ")", RestartRequired.withNote("Custom font", "customFontEnabled", NOTE));
        assertEquals("Ghost mode", RestartRequired.withNote("Ghost mode", "enableGhostMode", NOTE));
    }

    @Test
    public void noteIsNeverDoubled() {
        String once = RestartRequired.withNote("Story ring size", "customStoryRingSize", NOTE);
        assertEquals(once, RestartRequired.withNote(once, "customStoryRingSize", NOTE));
        assertEquals("Always Show Full Captions (restart IG)",
                RestartRequired.withNote("Always Show Full Captions (restart IG)", "expandCaptions", NOTE));
    }
}
