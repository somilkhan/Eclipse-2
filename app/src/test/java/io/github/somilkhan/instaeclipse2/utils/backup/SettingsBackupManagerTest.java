package io.github.somilkhan.instaeclipse2.utils.backup;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

import static org.junit.Assert.*;

/** Robolectric only for org.json (android.jar stubs it out in plain unit tests). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsBackupManagerTest {

    private final Map<Field, Object> saved = new HashMap<>();

    @Before public void snapshot() throws Exception {
        for (Field f : settingFields()) saved.put(f, f.get(null));
    }

    @After public void restore() throws Exception {
        for (Map.Entry<Field, Object> e : saved.entrySet()) e.getKey().set(null, e.getValue());
    }

    private static List<Field> settingFields() {
        List<Field> out = new ArrayList<>();
        for (Field f : FeatureFlags.class.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isPublic(m) && Modifier.isStatic(m) && !Modifier.isFinal(m)) out.add(f);
        }
        return out;
    }

    private static JSONObject settings() throws Exception {
        return new JSONObject(SettingsBackupManager.toJson()).getJSONObject("settings");
    }

    /** QA-01: fails as soon as a new FeatureFlags setting isn't covered by the backup. */
    @Test public void everyFeatureFlagIsBackedUpOrExplicitlyNeverRestored() throws Exception {
        JSONObject s = settings();
        List<String> missing = new ArrayList<>();
        for (Field f : settingFields()) {
            Class<?> t = f.getType();
            assertTrue("FeatureFlags." + f.getName() + " has a type the backup can't carry: " + t,
                    t == boolean.class || t == int.class || t == String.class || t == double.class);
            if (!s.has(f.getName()) && !SettingsRestore.NEVER_RESTORED.contains(f.getName())) missing.add(f.getName());
        }
        assertTrue("not in the settings backup: " + missing, missing.isEmpty());
    }

    @Test public void previouslyForgottenSettingsAreBackedUp() throws Exception {
        JSONObject s = settings();
        for (String k : new String[]{"lockDirectMessages", "lockWholeApp", "lockUseFingerprint", "themePaletteJson",
                "themePresetId", "navTabOrder", "downloaderQualityMaxShortSide", "autoClearCacheSizeMb",
                "savedOnProfile", "enableCopyComment", "enablePhotoZoom", "hideSpecificChats", "cacheStories",
                "removeMetaAI", "disableDoubleTapLike", "hideStoriesTray", "airplaneMode"})
            assertTrue(k, s.has(k));
    }

    @Test public void secretsDeviceKeysAndLockStateAreNotBackedUp() throws Exception {
        FeatureFlags.lockDirectPasscode = "hash";
        JSONObject s = settings();
        for (String k : new String[]{"lockDirectPasscode", "lockDirectSalt", "downloaderCustomUri", "downloaderCustomPath",
                "customFontPath", "customEmojiPath", "focusLockUntil", "isExtremeMode", "isGhostModeEnabled"})
            assertFalse(k, s.has(k));
    }

    @Test public void roundTripRestoresEveryType() throws Exception {
        FeatureFlags.lockDirectMessages = true;
        FeatureFlags.themePresetId = 7;
        FeatureFlags.navTabOrder = "FEED,DIRECT";
        FeatureFlags.spoofLat = 31.25;
        FeatureFlags.likeAnimation = "HEART";
        FeatureFlags.hideStoriesTray = true;
        String json = SettingsBackupManager.toJson();

        FeatureFlags.lockDirectMessages = false;
        FeatureFlags.themePresetId = 1;
        FeatureFlags.navTabOrder = "";
        FeatureFlags.spoofLat = 0;
        FeatureFlags.likeAnimation = "RANDOM";
        FeatureFlags.hideStoriesTray = false;
        SettingsBackupManager.fromJson(json);

        assertTrue(FeatureFlags.lockDirectMessages);
        assertEquals(7, FeatureFlags.themePresetId);
        assertEquals("FEED,DIRECT", FeatureFlags.navTabOrder);
        assertEquals(31.25, FeatureFlags.spoofLat, 0);
        assertEquals("HEART", FeatureFlags.likeAnimation);
        assertTrue(FeatureFlags.hideStoriesTray);
    }

    /** SEC-08: a wrongly typed value is skipped; nothing throws, nothing is half-applied. */
    @Test public void mistypedValuesAreSkippedNotThrown() {
        FeatureFlags.isGhostSeen = false;
        FeatureFlags.isGhostTyping = false;
        FeatureFlags.forceReelQuality = 0;
        SettingsBackupManager.fromJson("{\"settings\":{\"isGhostSeen\":true,\"isGhostTyping\":1,"
                + "\"forceReelQuality\":\"abc\",\"spoofLng\":\"nope\"}}");
        assertTrue(FeatureFlags.isGhostSeen);
        assertFalse(FeatureFlags.isGhostTyping);
        assertEquals(0, FeatureFlags.forceReelQuality);
    }

    @Test public void malformedFileChangesNothing() {
        FeatureFlags.isGhostSeen = false;
        try {
            SettingsBackupManager.fromJson("{\"isGhostSeen\":true");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { }
        assertFalse(FeatureFlags.isGhostSeen);
    }

    @Test public void invalidSanitisedValuesAreSkipped() {
        FeatureFlags.customShareDomain = "kk.example";
        FeatureFlags.startupTab = "FEED";
        SettingsBackupManager.fromJson("{\"customShareDomain\":\"not a host!\",\"startupTab\":\"BOGUS\"}");
        assertEquals("kk.example", FeatureFlags.customShareDomain);
        assertEquals("FEED", FeatureFlags.startupTab);
    }

    /** QA-04: restoring an older backup can't release Extreme Mode or its locked switches. */
    @Test public void extremeModeLockSurvivesRestore() {
        FeatureFlags.isExtremeMode = true;
        FeatureFlags.focusLockUntil = "";
        FeatureFlags.disableReels = true;
        FeatureFlags.isGhostSeen = false;
        SettingsBackupManager.fromJson("{\"isExtremeMode\":false,\"disableReels\":false,\"isGhostSeen\":true}");
        assertTrue(FeatureFlags.isExtremeMode);
        assertTrue(FeatureFlags.disableReels);
        assertTrue(FeatureFlags.isGhostSeen);
    }

    @Test public void backupCannotTurnExtremeModeOn() {
        FeatureFlags.isExtremeMode = false;
        FeatureFlags.disableReels = false;
        SettingsBackupManager.fromJson("{\"isExtremeMode\":true,\"disableReels\":true}");
        assertFalse(FeatureFlags.isExtremeMode);
        assertTrue(FeatureFlags.disableReels); // not locked: the distraction switch itself restores
    }

    @Test public void expiredFocusLockNoLongerBlocksRestore() {
        FeatureFlags.isExtremeMode = true;
        FeatureFlags.focusLockUntil = "1000"; // long past
        FeatureFlags.disableReels = true;
        SettingsBackupManager.fromJson("{\"disableReels\":false}");
        assertFalse(FeatureFlags.disableReels);
    }
}
