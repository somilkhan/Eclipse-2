package io.github.somilkhan.instaeclipse2.utils.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LoggingRedactionTest {

    @After public void tearDown() { Logging.clear(); }

    @Test public void appendRedactsUrlsBeforeBuffering() {
        Logging.clear();
        Logging.append("probeUrl=https://scontent.cdninstagram.com/v/t51.2885-15/abc.mp4?oh=SECRET&oe=1");
        Logging.append("plain");
        String snap = Logging.getSnapshot();
        assertFalse(snap.contains("SECRET"));
        assertFalse(snap.contains("abc.mp4"));
        assertTrue(snap.contains("probeUrl=https://scontent.cdninstagram.com/v/…"));
        assertTrue(snap.contains(" plain\n"));
        assertFalse(Logging.getSnapshotForIpc().contains("SECRET"));
    }

    @Test public void moduleLogRedactsThrowableText() {
        Logging.clear();
        ModuleLog.line("viewer load", new java.io.FileNotFoundException(
                "https://scontent.cdninstagram.com/v/t51.2885-19/p.jpg?oh=SECRET2"));
        String snap = Logging.getSnapshot();
        assertFalse(snap.contains("SECRET2"));
        assertTrue(snap.contains("FileNotFoundException: https://scontent.cdninstagram.com/v/…"));
    }

    @Test public void mirrorBudgetIsTwiceTheRing() {
        assertFalse(Logging.overMirrorBudget(Logging.MAX_MIRROR_BYTES));
        assertTrue(Logging.overMirrorBudget(Logging.MAX_MIRROR_BYTES + 1));
    }

    @Test public void mirrorPayloadKeepsNewestLinesWithinRingBudget() {
        char[] fill = new char[1000];
        Arrays.fill(fill, 'x');
        String body = new String(fill);
        List<String> lines = new ArrayList<>();
        // ~3x the 4 MB ring budget.
        for (int i = 0; i < 12_600; i++) lines.add(i + " " + body);
        byte[] payload = Logging.mirrorPayload(lines);
        assertTrue(payload.length <= Logging.MAX_MIRROR_BYTES / 2);
        assertTrue(payload.length > Logging.MAX_MIRROR_BYTES / 2 - 2000);
        String text = new String(payload, StandardCharsets.UTF_8);
        assertTrue(text.endsWith("12599 " + body + "\n"));
        // Starts on a whole line.
        assertTrue(text.matches("(?s)\\d+ x+\n.*"));
        assertEquals(0, Logging.mirrorPayload(new ArrayList<>()).length);
    }
}
