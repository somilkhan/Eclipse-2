package ps.reso.instaeclipse.utils.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure (Android-free) helpers for the Logs tab's "Save to storage" export: the diagnostic header,
 * the file name, and streaming the complete log sections into an output stream without any size
 * cap. Kept free of Android types so it is covered by plain JUnit tests.
 */
public final class LogExport {

    public static final String MIME_TYPE = "text/plain";
    public static final String SEPARATOR = "------------------------------------------------------------";
    private static final String UNKNOWN = "unknown";

    private LogExport() {}

    /** One installed Instagram flavor, as shown in the header. */
    public static final class TargetApp {
        public final String packageName;
        public final String versionName;
        public final long versionCode;

        public TargetApp(String packageName, String versionName, long versionCode) {
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }

    /** Values for the header; every field may be null/empty and is then printed as "unknown". */
    public static final class DeviceInfo {
        public String appVersionName;
        public long appVersionCode;
        public String buildType;
        public List<TargetApp> instagram;
        public String androidRelease;
        public int sdkInt;
        public String manufacturer;
        public String model;
        public String device;
        public String testedInstagram;
    }

    /** "instaeclipse-log-20261127-101500.txt" (local time). */
    public static String fileName(Date when, TimeZone zone) {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        f.setTimeZone(zone);
        return "instaeclipse-log-" + f.format(when) + ".txt";
    }

    public static String header(DeviceInfo info, Date when, TimeZone zone) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
        f.setTimeZone(zone);
        StringBuilder sb = new StringBuilder(512);
        sb.append("InstaEclipse log export\n");
        sb.append("Exported: ").append(f.format(when)).append('\n');
        sb.append("App version: ").append(appVersion(info)).append('\n');
        sb.append("Instagram version: ").append(instagramVersion(info.instagram)).append('\n');
        if (!isBlank(info.testedInstagram)) {
            sb.append("Tested Instagram version: ").append(info.testedInstagram).append('\n');
        }
        sb.append("Android version: ").append(androidVersion(info.androidRelease, info.sdkInt)).append('\n');
        sb.append("Device model: ").append(deviceModel(info.manufacturer, info.model, info.device)).append('\n');
        sb.append(SEPARATOR).append('\n');
        return sb.toString();
    }

    static String appVersion(DeviceInfo info) {
        if (isBlank(info.appVersionName)) return UNKNOWN;
        String s = info.appVersionName + " (" + info.appVersionCode + ")";
        if (!isBlank(info.buildType)) s += " " + info.buildType;
        return s;
    }

    static String instagramVersion(List<TargetApp> apps) {
        if (apps == null || apps.isEmpty()) return "not installed";
        StringBuilder sb = new StringBuilder();
        for (TargetApp a : apps) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(isBlank(a.versionName) ? UNKNOWN : a.versionName);
            if (a.versionCode > 0) sb.append(" (").append(a.versionCode).append(')');
            sb.append(" [").append(a.packageName).append(']');
        }
        return sb.toString();
    }

    static String androidVersion(String release, int sdk) {
        String r = isBlank(release) ? UNKNOWN : release;
        return sdk > 0 ? r + " (API " + sdk + ")" : r;
    }

    static String deviceModel(String manufacturer, String model, String device) {
        String m = isBlank(model) ? UNKNOWN : model.trim();
        if (!isBlank(manufacturer) && !m.toLowerCase(Locale.ROOT).startsWith(manufacturer.trim().toLowerCase(Locale.ROOT))) {
            m = capitalize(manufacturer.trim()) + " " + m;
        }
        if (!isBlank(device)) m += " (" + device.trim() + ")";
        return m;
    }

    /** Title line written before each log source. */
    public static String sectionTitle(String title) {
        return "\n===== " + title + " =====\n";
    }

    /** Streams {@code in} completely into {@code out}; returns the number of bytes copied. */
    public static long copyAll(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            total += n;
        }
        return total;
    }

    /** Writes text, ensuring it ends with a newline. */
    public static void writeText(OutputStream out, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        out.write(text.getBytes(StandardCharsets.UTF_8));
        if (text.charAt(text.length() - 1) != '\n') out.write('\n');
    }

    /**
     * Caps one log entry at {@code max} chars without splitting a surrogate pair, marking the cut.
     * Entries under the cap (including multi-line stack traces) are returned unchanged.
     */
    public static String clampEntry(String entry, int max) {
        if (entry == null || entry.length() <= max) return entry;
        int end = Character.isHighSurrogate(entry.charAt(max - 1)) ? max - 1 : max;
        return entry.substring(0, end) + " [truncated]";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
