package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstantSaveLongPressTest {
    @Test
    public void savesOnlyOnLongPressStart() {
        assertTrue(InstantSaveHook.isLongPressStart(new Object[]{Boolean.TRUE}));  // 457 threshold
        assertFalse(InstantSaveHook.isLongPressStart(new Object[]{Boolean.FALSE})); // 457 release
    }

    @Test
    public void olderBuildsWithoutBooleanStillSave() {
        assertTrue(InstantSaveHook.isLongPressStart(null));
        assertTrue(InstantSaveHook.isLongPressStart(new Object[0]));
        assertTrue(InstantSaveHook.isLongPressStart(new Object[]{new Object()}));
    }
}
