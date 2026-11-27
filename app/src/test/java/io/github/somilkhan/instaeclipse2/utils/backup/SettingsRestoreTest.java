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
}
