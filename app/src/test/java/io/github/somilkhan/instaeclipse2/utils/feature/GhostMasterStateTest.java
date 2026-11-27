package io.github.somilkhan.instaeclipse2.utils.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class GhostMasterStateTest {

    @After
    public void reset() {
        setAll(false);
    }

    @Test
    public void masterIsOnOnlyWhenEveryGhostFeatureIsOn() {
        setAll(true);
        assertTrue(FeatureFlags.ghostMasterState());

        FeatureFlags.permanentViewMode = false;
        assertFalse(FeatureFlags.ghostMasterState());

        setAll(false);
        assertFalse(FeatureFlags.ghostMasterState());
    }

    @Test
    public void lookupVariantMatchesTheMasterSwitchKeys() {
        Set<String> on = new HashSet<>(FeatureFlags.GHOST_MASTER_KEYS);
        assertTrue(FeatureFlags.ghostMasterState(on::contains));

        on.remove("isGhostTyping");
        assertFalse(FeatureFlags.ghostMasterState(on::contains));
    }

    @Test
    public void bothVariantsCoverTheSameEightFeatures() {
        assertEquals(8, FeatureFlags.GHOST_MASTER_KEYS.size());
        // Turning off any single master key must turn the live-flag master off too.
        for (String key : FeatureFlags.GHOST_MASTER_KEYS) {
            setAll(true);
            setFlag(key, false);
            assertFalse(key, FeatureFlags.ghostMasterState());
        }
    }

    private static void setAll(boolean value) {
        for (String key : FeatureFlags.GHOST_MASTER_KEYS) setFlag(key, value);
    }

    private static void setFlag(String key, boolean value) {
        try {
            FeatureFlags.class.getField(key).setBoolean(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("FeatureFlags has no boolean field " + key, e);
        }
    }
}
