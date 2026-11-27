package io.github.somilkhan.instaeclipse2.utils.log;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

public class LogExportTest {

    private static final TimeZone IST = TimeZone.getTimeZone("Asia/Kolkata");
    private static final Date WHEN = new Date(1795753800000L); // 2026-11-27 10:00:00 IST

    private static LogExport.DeviceInfo info() {
        LogExport.DeviceInfo i = new LogExport.DeviceInfo();
        i.appVersionName = "0.8.4";
        i.appVersionCode = 22;
        i.buildType = "release";
        i.instagram = Collections.singletonList(
                new LogExport.TargetApp("com.instagram.android", "457.0.0.55.73", 386314341L));
        i.testedInstagram = "457.0.0.55.73";
        i.androidRelease = "14";
        i.sdkInt = 34;
        i.manufacturer = "Google";
        i.model = "Pixel 8";
        i.device = "shiba";
        return i;
    }

    @Test
    public void headerHasAllDiagnosticFields() {
        String h = LogExport.header(info(), WHEN, IST);
        assertTrue(h, h.startsWith("InstaEclipse log export\n"));
        assertTrue(h, h.contains("Exported: 2026-11-27 10:00:00 +0530\n"));
        assertTrue(h, h.contains("App version: 0.8.4 (22) release\n"));
        assertTrue(h, h.contains("Instagram version: 457.0.0.55.73 (386314341) [com.instagram.android]\n"));
        assertTrue(h, h.contains("Tested Instagram version: 457.0.0.55.73\n"));
        assertTrue(h, h.contains("Android version: 14 (API 34)\n"));
        assertTrue(h, h.contains("Device model: Google Pixel 8 (shiba)\n"));
        assertTrue(h, h.endsWith(LogExport.SEPARATOR + "\n"));
    }

    @Test
    public void headerToleratesMissingValues() {
        String h = LogExport.header(new LogExport.DeviceInfo(), WHEN, IST);
        assertTrue(h, h.contains("App version: unknown\n"));
        assertTrue(h, h.contains("Instagram version: not installed\n"));
        assertTrue(h, h.contains("Android version: unknown\n"));
        assertTrue(h, h.contains("Device model: unknown\n"));
        assertTrue(h, !h.contains("Tested Instagram"));
    }

    @Test
    public void multipleInstagramFlavorsAreListed() {
        String v = LogExport.instagramVersion(Arrays.asList(
                new LogExport.TargetApp("com.instagram.android", "457.0.0.55.73", 1),
                new LogExport.TargetApp("com.instagold.android", null, 0)));
        assertEquals("457.0.0.55.73 (1) [com.instagram.android]; unknown [com.instagold.android]", v);
    }

    @Test
    public void manufacturerNotRepeatedWhenModelAlreadyHasIt() {
        assertEquals("Samsung SM-S918B", LogExport.deviceModel("samsung", "SM-S918B", null));
        assertEquals("Google Pixel 8 Pro", LogExport.deviceModel("Google", "Pixel 8 Pro", ""));
        assertEquals("OnePlus 12 (aston)", LogExport.deviceModel("OnePlus", "OnePlus 12", "aston"));
    }

    @Test
    public void fileNameIsTimestampedTxt() {
        assertEquals("instaeclipse-log-20261127-100000.txt", LogExport.fileName(WHEN, IST));
    }

    @Test
    public void copyAllHasNoSizeCap() throws Exception {
        byte[] big = new byte[9 * 1024 * 1024 + 123]; // larger than any view / clipboard cap
        for (int i = 0; i < big.length; i++) big[i] = (byte) ('a' + (i % 26));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(big.length, LogExport.copyAll(new ByteArrayInputStream(big), out));
        assertArrayEquals(big, out.toByteArray());
    }

    @Test
    public void writeTextTerminatesLines() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LogExport.writeText(out, "a");
        LogExport.writeText(out, "b\n");
        LogExport.writeText(out, null);
        LogExport.writeText(out, "");
        assertEquals("a\nb\n", out.toString(StandardCharsets.UTF_8.name()));
        assertEquals("\n===== X =====\n", LogExport.sectionTitle("X"));
    }

    @Test
    public void clampKeepsEntriesUnderTheCapIntact() {
        StringBuilder trace = new StringBuilder("java.lang.IllegalStateException: boom");
        while (trace.length() < 20_000) trace.append("\n\tat com.instagram.Foo.bar(Unknown Source:12)");
        String s = trace.toString();
        assertSame(s, LogExport.clampEntry(s, 32 * 1024)); // old 4096 cap cut these short
        assertEquals(null, LogExport.clampEntry(null, 10));
    }

    @Test
    public void clampMarksCutAndNeverSplitsSurrogatePairs() {
        assertEquals("abcde [truncated]", LogExport.clampEntry("abcdefgh", 5));
        String emoji = "abcd\uD83D\uDE00xyz"; // surrogate pair at index 4..5
        String c = LogExport.clampEntry(emoji, 5);
        assertEquals("abcd [truncated]", c);
    }
}
