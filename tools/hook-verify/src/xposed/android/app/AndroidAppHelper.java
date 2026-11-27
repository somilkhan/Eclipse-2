package android.app;

/** Host-JVM stand-in: there is no running Instagram application in the harness. */
public final class AndroidAppHelper {
    private AndroidAppHelper() {}
    public static String currentProcessName() { return "com.instagram.android"; }
    public static android.content.pm.ApplicationInfo currentApplicationInfo() { return null; }
    public static String currentPackageName() { return "com.instagram.android"; }
    public static Application currentApplication() { return null; }
    public static android.content.SharedPreferences getSharedPreferencesForPackage(String p, String n, int m) { return null; }
    public static android.content.SharedPreferences getDefaultSharedPreferencesForPackage(String p) { return null; }
    public static void reloadSharedPreferencesIfNeeded(android.content.SharedPreferences p) {}
}
