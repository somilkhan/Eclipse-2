package android.util;

import hookverify.Recorder;

/** Host-JVM android.util.Log: routes every line into the harness recorder. */
public final class Log {
    public static final int VERBOSE = 2, DEBUG = 3, INFO = 4, WARN = 5, ERROR = 6, ASSERT = 7;
    private Log() {}
    private static int out(String tag, String msg, Throwable tr) {
        Recorder.log(msg + (tr != null ? " | " + tr : ""));
        return 0;
    }
    public static int v(String tag, String msg) { return out(tag, msg, null); }
    public static int v(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int d(String tag, String msg) { return out(tag, msg, null); }
    public static int d(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int i(String tag, String msg) { return out(tag, msg, null); }
    public static int i(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int w(String tag, String msg) { return out(tag, msg, null); }
    public static int w(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int w(String tag, Throwable tr) { return out(tag, "", tr); }
    public static int e(String tag, String msg) { return out(tag, msg, null); }
    public static int e(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int wtf(String tag, String msg) { return out(tag, msg, null); }
    public static int wtf(String tag, String msg, Throwable tr) { return out(tag, msg, tr); }
    public static int wtf(String tag, Throwable tr) { return out(tag, "", tr); }
    public static boolean isLoggable(String tag, int level) { return true; }
    public static int println(int priority, String tag, String msg) { return out(tag, msg, null); }
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        tr.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
