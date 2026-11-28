package io.github.somilkhan.instaeclipse2.utils.backup;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class SettingsRestoreTest {

    @Test public void restoresIntegerAndStringSettingsNotOnlyBooleans() {
        Map<String, Object> out = SettingsRestore.parse("{\"version\":1,\"settings\":{"
                + "\"isGhostSeen\":true,\"forceReelQuality\":1080,\"spoofLat\":\"31.5\","
                + "\"startupTab\":\"FEED\"}}");
        assertEquals(Boolean.TRUE, out.get("isGhostSeen"));
        assertEquals(1080, out.get("forceReelQuality"));
        assertEquals("31.5", out.get("spoofLat"));
        assertEquals("FEED", out.get("startupTab"));
    }

    @Test public void numericCoordinatesAreKeptAsTheirStoredStringType() {
        Map<String, Object> out = SettingsRestore.parse("{\"spoofLat\":31.25,\"spoofLng\":35}");
        assertEquals("31.25", out.get("spoofLat"));
        assertEquals("35", out.get("spoofLng"));
    }

    @Test public void unknownMistypedAndDeviceSpecificKeysAreDropped() {
        Map<String, Object> out = SettingsRestore.parse("{\"settings\":{"
                + "\"version\":1,\"synthetic_unknown\":true,\"isGhostSeen\":\"yes\","
                + "\"forceReelQuality\":true,\"forceReelQuality2\":1,"
                + "\"lockDirectPasscode\":\"abc\",\"lockDirectSalt\":\"x\","
                + "\"downloaderCustomUri\":\"content://x\",\"downloaderCustomPath\":\"/x\","
                + "\"isAdBlockEnabled\":{\"nested\":true}}}");
        assertTrue(out.toString(), out.isEmpty());
    }

    @Test public void flatFormatIsAccepted() {
        assertEquals(Boolean.FALSE, SettingsRestore.parse("{\"isAdBlockEnabled\":false}").get("isAdBlockEnabled"));
    }

    @Test(expected = IllegalArgumentException.class) public void nonObjectIsRejected() {
        SettingsRestore.parse("[true]");
    }

    @Test(expected = IllegalArgumentException.class) public void garbageIsRejected() {
        SettingsRestore.parse("{not json");
    }

    @Test public void actionMatchesValueType() {
        assertEquals("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF", SettingsRestore.actionFor(true));
        assertEquals("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_INT", SettingsRestore.actionFor(5));
        assertEquals("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_STRING", SettingsRestore.actionFor("x"));
    }

    @Test public void extremeModeAndDerivedKeysAreNeverRestored() {
        Map<String, Object> out = SettingsRestore.parse("{\"isExtremeMode\":false,\"isDistractionFree\":false,"
                + "\"isGhostModeEnabled\":true,\"focusLockUntil\":\"1\",\"disableReels\":false}");
        assertEquals(java.util.Collections.singletonMap("disableReels", false), out);
    }

    @Test public void lockedRestoreDropsOnlyTheDistractionSwitches() {
        Map<String, Object> in = SettingsRestore.parse("{\"disableStories\":false,\"disableFeed\":false,"
                + "\"disableReels\":false,\"disableReelsExceptDM\":false,\"disableExplore\":false,"
                + "\"disableComments\":false,\"isGhostSeen\":true}");
        assertEquals(java.util.Collections.singletonMap("isGhostSeen", true), SettingsRestore.filterLocked(in, true));
        assertEquals(in, SettingsRestore.filterLocked(in, false));
    }

    @Test public void lockStateFollowsExtremeModeAndFocusLockExpiry() {
        assertTrue(SettingsRestore.isLocked(true, "", 5_000));          // permanent Extreme Mode
        assertTrue(SettingsRestore.isLocked(true, "10000", 5_000));     // Focus Lock still running
        assertFalse(SettingsRestore.isLocked(true, "1000", 5_000));     // Focus Lock over
        assertFalse(SettingsRestore.isLocked(false, "10000", 5_000));
    }

    /** QA-08: companion-only prefs come back through their own typed path, never broadcast. */
    @Test public void companionLocalPrefsAreRestoredSeparately() {
        String json = "{\"settings\":{\"crashReports\":false,\"homePinnedShortcuts\":\"isGhostSeen\","
                + "\"isGhostSeen\":true}}";
        Map<String, Object> local = SettingsRestore.parseCompanionLocal(json);
        assertEquals(Boolean.FALSE, local.get("crashReports"));
        assertEquals("isGhostSeen", local.get("homePinnedShortcuts"));
        assertEquals(2, local.size());
        assertFalse(SettingsRestore.parse(json).containsKey("crashReports"));
        assertTrue(SettingsRestore.parseCompanionLocal("{\"crashReports\":\"no\",\"homePinnedShortcuts\":3}").isEmpty());
    }

    /** QA-02: the companion backup keeps ints and strings, not only Booleans. */
    @Test public void companionBackupKeepsEveryKnownTypeAndDropsStampsAndSecrets() {
        Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("isGhostSeen", true);
        prefs.put("storyRingSizeDp", 120);
        prefs.put("likeAnimation", "HEART");
        prefs.put("spoofLat", "31.5");
        prefs.put("crashReports", false);
        prefs.put("__ts_isGhostSeen", 123L);
        prefs.put("__applied_isGhostSeen", 123L);
        prefs.put("lockDirectPasscode", "hash");
        prefs.put("downloaderCustomUri", "content://x");
        prefs.put("isExtremeMode", true);
        prefs.put("forceReelQuality", "wrong type");
        prefs.put("synthetic_unknown", true);
        Map<String, Object> out = SettingsRestore.backupEntries(prefs);
        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList(
                "isGhostSeen", "storyRingSizeDp", "likeAnimation", "spoofLat", "crashReports")), out.keySet());

        // ... and what it writes comes back unchanged.
        com.google.gson.JsonObject settings = new com.google.gson.Gson().toJsonTree(out).getAsJsonObject();
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("version", 1);
        root.add("settings", settings);
        Map<String, Object> back = new java.util.TreeMap<>(SettingsRestore.parse(root.toString()));
        back.putAll(SettingsRestore.parseCompanionLocal(root.toString()));
        assertEquals(out, back);
    }
}
