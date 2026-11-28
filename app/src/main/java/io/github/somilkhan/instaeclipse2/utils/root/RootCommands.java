package io.github.somilkhan.instaeclipse2.utils.root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds the only shell scripts the companion ever runs as root. Pure Java (no Android) so the
 * quoting, the whitelist and the path layout are covered by JVM tests.
 *
 * <p>Rules every builder follows:
 * <ul>
 *   <li>The command words are constants here; nothing from the user, a setting or Instagram is
 *   ever used as a command.</li>
 *   <li>A package must be in the supported-package whitelist (passed in by the caller, normally
 *   {@code CommonUtils.SUPPORTED_PACKAGES}) and look like a package name; anything else throws.</li>
 *   <li>Paths are built from that package, a numeric user id and constant relative paths. The one
 *   variable file name (a pending crash report, listed from Instagram's own data dir and so
 *   Instagram-controlled) must match {@link #CRASH_FILE}.</li>
 *   <li>Every argument is single-quoted with {@link #quote}.</li>
 * </ul>
 */
public final class RootCommands {

    /** Same shape as CrashReporter's file names plus ".txt" (CrashReportProvider's NAME pattern). */
    public static final Pattern CRASH_FILE = Pattern.compile("crash_[A-Za-z0-9._-]{1,120}\\.txt");
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");

    /** Module files inside the hooked app's data dir (see Module, Logging, CrashReporter, DexKitCache). */
    public static final String MODULE_LOG = "files/eclipse2_module.log";
    public static final String CRASH_PENDING_DIR = "files/eclipse2_crash_pending";
    public static final String DEXKIT_CACHE_PREFS = "shared_prefs/eclipse2_dexkit_cache.xml";

    /** Same cap as the Logs tab reads from the mirror (LoggingFragment.MAX_READ_BYTES). */
    public static final long MAX_LOG_BYTES = 8L * 1024 * 1024;
    /** UserHandle.PER_USER_RANGE. */
    static final int PER_USER_RANGE = 100_000;

    /** Marker echoed by {@link #checkScript()} so a broken su that exits 0 is not taken as root. */
    public static final String CHECK_MARKER = "eclipse2-root-ok";

    private RootCommands() {}

    /** POSIX single-quoting: {@code it's} becomes {@code 'it'\''s'}. NUL can't be passed and throws. */
    public static String quote(String s) {
        if (s == null) throw new IllegalArgumentException("null argument");
        if (s.indexOf('\0') >= 0) throw new IllegalArgumentException("NUL in argument");
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /** The package, checked against the whitelist and the package-name shape. */
    public static String checkedPackage(String pkg, Collection<String> allowed) {
        if (pkg == null || allowed == null || !allowed.contains(pkg) || !PACKAGE.matcher(pkg).matches()) {
            throw new IllegalArgumentException("package not allowed: " + pkg);
        }
        return pkg;
    }

    public static int userIdOf(int uid) {
        return uid < 0 ? 0 : uid / PER_USER_RANGE;
    }

    /** {@code /data/user/<id>/<pkg>}; {@code /data/data} is only the user-0 alias. */
    public static String dataDir(String pkg, int userId, Collection<String> allowed) {
        if (userId < 0) throw new IllegalArgumentException("bad user id " + userId);
        return "/data/user/" + userId + "/" + checkedPackage(pkg, allowed);
    }

    /** Prints the effective uid and a marker; {@link #isGranted} checks both. */
    public static String checkScript() {
        return "id -u; echo " + CHECK_MARKER;
    }

    public static boolean isGranted(int exitCode, String stdout) {
        if (exitCode != 0 || stdout == null) return false;
        String[] lines = stdout.trim().split("\\s*\\n\\s*");
        return lines.length >= 2 && "0".equals(lines[0].trim()) && CHECK_MARKER.equals(lines[lines.length - 1].trim());
    }

    public static String forceStop(String pkg, int userId, Collection<String> allowed) {
        return "am force-stop --user " + userId + " " + quote(checkedPackage(pkg, allowed));
    }

    /**
     * Stops the app first (otherwise it writes its in-memory copy of the prefs back), then removes
     * the cache file and its backup. DexKitCache.init then finds no version stamp and rebuilds.
     */
    public static String clearDexKitCache(String pkg, int userId, Collection<String> allowed) {
        String file = dataDir(pkg, userId, allowed) + "/" + DEXKIT_CACHE_PREFS;
        return forceStop(pkg, userId, allowed) + " && rm -f " + quote(file) + " " + quote(file + ".bak");
    }

    /** The newest {@link #MAX_LOG_BYTES} of the module log; prints nothing if there is none. */
    public static String readModuleLog(String pkg, int userId, Collection<String> allowed) {
        String file = dataDir(pkg, userId, allowed) + "/" + MODULE_LOG;
        return "if [ -f " + quote(file) + " ]; then tail -c " + MAX_LOG_BYTES + " " + quote(file) + "; fi";
    }

    public static String listPendingCrashes(String pkg, int userId, Collection<String> allowed) {
        String dir = dataDir(pkg, userId, allowed) + "/" + CRASH_PENDING_DIR;
        return "if [ -d " + quote(dir) + " ]; then ls -1 " + quote(dir) + "; fi";
    }

    /** Keeps only well-formed crash file names from an {@code ls} listing (Instagram-controlled). */
    public static List<String> parseCrashListing(String stdout) {
        List<String> out = new ArrayList<>();
        if (stdout == null) return out;
        for (String line : stdout.split("\n")) {
            String name = line.trim();
            if (CRASH_FILE.matcher(name).matches() && !out.contains(name)) out.add(name);
        }
        return out;
    }

    public static String crashPath(String pkg, int userId, String fileName, Collection<String> allowed) {
        if (fileName == null || !CRASH_FILE.matcher(fileName).matches()) {
            throw new IllegalArgumentException("bad crash file name: " + fileName);
        }
        return dataDir(pkg, userId, allowed) + "/" + CRASH_PENDING_DIR + "/" + fileName;
    }

    /** Size-capped like CrashReporter.flushPending (4 x the provider's text cap). */
    public static String readCrash(String pkg, int userId, String fileName, long maxBytes, Collection<String> allowed) {
        return "head -c " + maxBytes + " " + quote(crashPath(pkg, userId, fileName, allowed));
    }

    /** Run only after the report was saved, so Instagram doesn't deliver it a second time. */
    public static String deleteCrash(String pkg, int userId, String fileName, Collection<String> allowed) {
        return "rm -f " + quote(crashPath(pkg, userId, fileName, allowed));
    }

    // ── Path selection ───────────────────────────────────────────────────────

    /** Which way an action ran; the UI names it in its result message. */
    public enum Path { ROOT, FALLBACK }

    /**
     * Root only when the user turned it on and it has not been found missing or denied. UNKNOWN
     * (not checked yet in this process) still tries root: the attempt itself is the check.
     */
    public static Path choose(boolean enabled, RootStatus status) {
        if (!enabled || status == RootStatus.NOT_ROOTED || status == RootStatus.DENIED) return Path.FALLBACK;
        return Path.ROOT;
    }

    /** Short, stable label for logs. */
    public static String describe(Path path, RootStatus status, boolean enabled) {
        return path.name().toLowerCase(Locale.ROOT) + " (root " + (enabled ? "on" : "off") + ", " + status + ")";
    }
}
