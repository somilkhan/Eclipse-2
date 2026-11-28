package io.github.somilkhan.instaeclipse2.utils.root;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RootCommandsTest {

    private static final List<String> ALLOWED = Arrays.asList("com.instagram.android", "cc.honista.app", "com.Instander.android");

    @Test public void quoteWrapsAndEscapesSingleQuotes() {
        assertEquals("'abc'", RootCommands.quote("abc"));
        assertEquals("''", RootCommands.quote(""));
        assertEquals("'it'\\''s'", RootCommands.quote("it's"));
        // Shell metacharacters stay literal inside single quotes.
        assertEquals("'a; rm -rf / $(id) `x` \"q\" \\n'", RootCommands.quote("a; rm -rf / $(id) `x` \"q\" \\n"));
    }

    @Test(expected = IllegalArgumentException.class) public void quoteRejectsNul() {
        RootCommands.quote("a\0b");
    }

    @Test(expected = IllegalArgumentException.class) public void quoteRejectsNull() {
        RootCommands.quote(null);
    }

    @Test public void packageMustBeWhitelisted() {
        assertEquals("cc.honista.app", RootCommands.checkedPackage("cc.honista.app", ALLOWED));
        for (String bad : new String[]{null, "", "com.evil.app", "com.instagram.android; reboot",
                "com.instagram.android\n", "../../system", "com.instagram.android'"}) {
            try {
                RootCommands.checkedPackage(bad, ALLOWED);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void whitelistedButMalformedNameIsStillRejected() {
        try {
            RootCommands.checkedPackage("x'; reboot; '", Arrays.asList("x'; reboot; '"));
            fail();
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void userIdFromUid() {
        assertEquals(0, RootCommands.userIdOf(10123));
        assertEquals(10, RootCommands.userIdOf(1010123));
        assertEquals(0, RootCommands.userIdOf(-1));
    }

    @Test public void forceStopIsQuotedAndUserScoped() {
        assertEquals("am force-stop --user 10 'com.Instander.android'",
                RootCommands.forceStop("com.Instander.android", 10, ALLOWED));
    }

    @Test public void clearDexKitStopsFirstThenRemovesPrefsAndBackup() {
        String s = RootCommands.clearDexKitCache("com.instagram.android", 0, ALLOWED);
        assertEquals("am force-stop --user 0 'com.instagram.android' && rm -f "
                + "'/data/user/0/com.instagram.android/shared_prefs/eclipse2_dexkit_cache.xml' "
                + "'/data/user/0/com.instagram.android/shared_prefs/eclipse2_dexkit_cache.xml.bak'", s);
    }

    @Test public void readModuleLogIsCappedAndQuoted() {
        String s = RootCommands.readModuleLog("cc.honista.app", 0, ALLOWED);
        assertTrue(s, s.contains("tail -c " + RootCommands.MAX_LOG_BYTES + " '/data/user/0/cc.honista.app/files/eclipse2_module.log'"));
        assertTrue(s, s.startsWith("if [ -f '"));
    }

    @Test public void crashListingKeepsOnlyWellFormedNames() {
        String ls = "crash_com.instagram.android_20261128_101500_java.txt\n"
                + "notes.txt\n"
                + "crash_x.txt; reboot\n"
                + "crash_$(id).txt\n"
                + "crash_../../etc.txt\n"   // '/' not allowed
                + "  crash_ok-1.txt  \n"
                + "crash_com.instagram.android_20261128_101500_java.txt\n"; // duplicate
        assertEquals(Arrays.asList("crash_com.instagram.android_20261128_101500_java.txt", "crash_ok-1.txt"),
                RootCommands.parseCrashListing(ls));
        assertTrue(RootCommands.parseCrashListing(null).isEmpty());
    }

    @Test public void crashCommandsRejectInstagramControlledNames() {
        for (String bad : new String[]{"crash_a.txt'; reboot; '", "../crash_a.txt", "crash_/a.txt", "a.txt", null}) {
            try {
                RootCommands.readCrash("com.instagram.android", 0, bad, 100, ALLOWED);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) { }
            try {
                RootCommands.deleteCrash("com.instagram.android", 0, bad, ALLOWED);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) { }
        }
        assertEquals("rm -f '/data/user/0/com.instagram.android/files/eclipse2_crash_pending/crash_a.txt'",
                RootCommands.deleteCrash("com.instagram.android", 0, "crash_a.txt", ALLOWED));
        assertEquals("head -c 640000 '/data/user/0/com.instagram.android/files/eclipse2_crash_pending/crash_a.txt'",
                RootCommands.readCrash("com.instagram.android", 0, "crash_a.txt", 640000, ALLOWED));
    }

    @Test public void grantNeedsUidZeroAndMarker() {
        assertTrue(RootCommands.isGranted(0, "0\n" + RootCommands.CHECK_MARKER + "\n"));
        assertFalse(RootCommands.isGranted(0, "10123\n" + RootCommands.CHECK_MARKER));
        assertFalse(RootCommands.isGranted(0, "0\n"));
        assertFalse(RootCommands.isGranted(1, "0\n" + RootCommands.CHECK_MARKER));
        assertFalse(RootCommands.isGranted(0, null));
    }

    @Test public void fallbackSelection() {
        assertEquals(RootCommands.Path.FALLBACK, RootCommands.choose(false, RootStatus.GRANTED));
        assertEquals(RootCommands.Path.FALLBACK, RootCommands.choose(false, RootStatus.UNKNOWN));
        assertEquals(RootCommands.Path.FALLBACK, RootCommands.choose(true, RootStatus.NOT_ROOTED));
        assertEquals(RootCommands.Path.FALLBACK, RootCommands.choose(true, RootStatus.DENIED));
        assertEquals(RootCommands.Path.ROOT, RootCommands.choose(true, RootStatus.UNKNOWN));
        assertEquals(RootCommands.Path.ROOT, RootCommands.choose(true, RootStatus.GRANTED));
    }
}
