package io.github.somilkhan.instaeclipse2.utils.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LogRedactTest {

    private static final String CDN =
            "https://scontent-bom1-1.cdninstagram.com/v/t51.2885-15/123456789_987654321_n.jpg"
                    + "?stp=dst-jpg&_nc_ht=x&oh=00_SECRETSIG&oe=6750ABCD";

    @Test public void signedCdnUrlLosesQueryAndFilename() {
        String r = LogRedact.text("(IE|Reel) media tag hit, url=" + CDN + " done");
        assertEquals("(IE|Reel) media tag hit, url=https://scontent-bom1-1.cdninstagram.com/v/… done", r);
        assertFalse(r.contains("SECRETSIG"));
        assertFalse(r.contains("oe="));
        assertFalse(r.contains("123456789"));
    }

    @Test public void shareLinkLosesShortcodeAndTracking() {
        String r = LogRedact.url("https://www.instagram.com/p/DAbCdEfGh12/?igsh=MzRlODBiNWFlZA==");
        assertEquals("https://www.instagram.com/p/…", r);
    }

    @Test public void profileUrlLosesUsername() {
        assertEquals("https://www.instagram.com/…", LogRedact.url("https://www.instagram.com/some.user/"));
        assertEquals("https://www.instagram.com/…", LogRedact.url("https://www.instagram.com/some.user"));
    }

    @Test public void userinfoAndFragmentDropped() {
        assertEquals("https://host.example/…", LogRedact.text("https://user:pw@host.example/a?b#c"));
        assertEquals("https://host.example/…", LogRedact.text("https://host.example?token=abc"));
        assertEquals("https://host.example", LogRedact.text("https://host.example"));
        assertEquals("https://host.example/", LogRedact.text("https://host.example/"));
    }

    @Test public void customSchemesRedactedButLocalSchemesKept() {
        assertEquals("instagram://media/…",
                LogRedact.text("instagram://media?id=123_456"));
        String local = "content://io.github.somilkhan.instaeclipse2.logmirror/log file:///data/x.log";
        assertSame(local, LogRedact.text(local));
    }

    @Test public void multipleUrlsAndStackTraceText() {
        String trace = "java.io.FileNotFoundException: " + CDN + "\n\tat a.b(C.java:1)\nprobe=" + CDN;
        String r = LogRedact.text(trace);
        assertFalse(r.contains("SECRETSIG"));
        assertTrue(r.contains("\n\tat a.b(C.java:1)\n"));
        assertEquals(2, r.split("cdninstagram.com/v/…", -1).length - 1);
    }

    @Test public void idempotent() {
        String once = LogRedact.text("x " + CDN + " https://www.instagram.com/p/abc/ y");
        assertEquals(once, LogRedact.text(once));
        assertEquals(once, LogRedact.text(LogRedact.text(once)));
    }

    @Test public void noUrlFastPathReturnsSameInstance() {
        String s = "plain line without links";
        assertSame(s, LogRedact.text(s));
        assertEquals(null, LogRedact.text(null));
    }

    @Test public void idIsShortStableAndHidesValue() {
        String a = LogRedact.id("some.user");
        assertTrue(a.matches("#[0-9a-f]{8}"));
        assertEquals(a, LogRedact.id("some.user"));
        assertNotEquals(a, LogRedact.id("other.user"));
        assertEquals("null", LogRedact.id(null));
        assertEquals("\"\"", LogRedact.id(""));
        assertFalse(LogRedact.id(3141592653589793238L).contains("3141592653"));
    }

    @Test public void urlHelperHandlesNullAndNonUrl() {
        assertEquals("null", LogRedact.url(null));
        assertTrue(LogRedact.url("not-a-url-but-maybe-a-code").matches("#[0-9a-f]{8}"));
    }
}
