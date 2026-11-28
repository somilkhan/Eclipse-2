package io.github.somilkhan.instaeclipse2.utils.root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The root-powered actions, each with the path it took so the UI can say so. Pure Java: the
 * Android parts (launching Instagram, opening App info, writing a crash report to the chosen
 * folder) stay with the caller. Blocking; run on {@link RootAccess#executor()}.
 *
 * <p>Why each action needs root (what the companion can't do today):
 * <ul>
 *   <li>{@link #forceStop}: an app can't stop another app; without root the user has to press
 *   Force stop in App info. Restart-required settings ({@code RestartRequired}) only apply after
 *   that.</li>
 *   <li>{@link #clearDexKitCache}: the cache is in Instagram's own data dir; without root it can
 *   only be cleared from the Eclipse menu inside Instagram.</li>
 *   <li>{@link #readModuleLog}: the Logs tab reads a mirror Instagram writes through
 *   {@code LogMirrorProvider}; when Instagram can't open it (started before the companion was
 *   installed or updated, provider blocked) and isn't running to answer the live request, the
 *   log is only in Instagram's data dir.</li>
 *   <li>{@link #collectCrashes}: crash reports Instagram couldn't hand to
 *   {@code CrashReportProvider} wait in its data dir until Instagram starts again. A crash loop
 *   or an Instagram that won't start never delivers them.</li>
 * </ul>
 */
public final class RootActions {

    /** Same cap as CrashReporter.flushPending (4 x CrashReportProvider.MAX_TEXT_CHARS). */
    public static final long MAX_CRASH_BYTES = 4L * 160_000;

    /** Result of one action. */
    public static final class Outcome {
        public final RootCommands.Path path;
        /** For ROOT: the command succeeded. FALLBACK outcomes are always "not done yet". */
        public final boolean ok;
        /** FALLBACK because root was tried and failed (vs. not enabled / not available). */
        public final boolean rootFailed;
        public final int count;

        Outcome(RootCommands.Path path, boolean ok, boolean rootFailed, int count) {
            this.path = path;
            this.ok = ok;
            this.rootFailed = rootFailed;
            this.count = count;
        }

        static Outcome fallback(boolean rootFailed) {
            return new Outcome(RootCommands.Path.FALLBACK, false, rootFailed, 0);
        }

        static Outcome root(int count) {
            return new Outcome(RootCommands.Path.ROOT, true, false, count);
        }
    }

    /** Saves one pending crash report the normal way (CrashReportProvider's folders). */
    public interface CrashSink {
        /** @return true when saved; the pending file is only deleted then. */
        boolean save(String name, String text);
    }

    private final RootSession session;
    private final int userId;
    private final Collection<String> allowed;

    public RootActions(RootSession session, int userId, Collection<String> allowedPackages) {
        this.session = session;
        this.userId = userId;
        this.allowed = allowedPackages;
    }

    public Outcome forceStop(String pkg) {
        return simple("force-stop " + pkg, RootCommands.forceStop(pkg, userId, allowed));
    }

    public Outcome clearDexKitCache(String pkg) {
        return simple("clear DexKit cache of " + pkg, RootCommands.clearDexKitCache(pkg, userId, allowed));
    }

    /** The module log from Instagram's data dir, or {@code null} for the non-root path. */
    public String readModuleLog(String pkg) {
        ShellRunner.Result r = session.run("read log of " + pkg,
                RootCommands.readModuleLog(pkg, userId, allowed), RootSession.ACTION_TIMEOUT_MS);
        return r != null && r.ok() ? r.stdout : null;
    }

    /**
     * Saves every pending crash report of {@code pkg} through {@code sink}, deleting each one only
     * after it was saved. FALLBACK means Instagram has to deliver them on its next start.
     */
    public Outcome collectCrashes(String pkg, CrashSink sink) {
        ShellRunner.Result list = session.run("list crash reports of " + pkg,
                RootCommands.listPendingCrashes(pkg, userId, allowed), RootSession.ACTION_TIMEOUT_MS);
        if (list == null) return Outcome.fallback(false);
        if (!list.ok()) return Outcome.fallback(true);
        int saved = 0;
        boolean failed = false;
        for (String file : RootCommands.parseCrashListing(list.stdout)) {
            ShellRunner.Result body = session.run("read " + file,
                    RootCommands.readCrash(pkg, userId, file, MAX_CRASH_BYTES, allowed), RootSession.ACTION_TIMEOUT_MS);
            if (body == null || !body.ok() || body.stdout.isEmpty()) { failed = true; continue; }
            String name = file.substring(0, file.length() - ".txt".length());
            if (!sink.save(name, body.stdout)) { failed = true; continue; }
            saved++;
            ShellRunner.Result rm = session.run("delete delivered " + file,
                    RootCommands.deleteCrash(pkg, userId, file, allowed), RootSession.ACTION_TIMEOUT_MS);
            if (rm == null || !rm.ok()) failed = true; // saved, but Instagram may deliver it again
        }
        return new Outcome(RootCommands.Path.ROOT, !failed, false, saved);
    }

    private Outcome simple(String what, String script) {
        ShellRunner.Result r = session.run(what, script, RootSession.ACTION_TIMEOUT_MS);
        if (r == null) return Outcome.fallback(false);
        return r.ok() ? Outcome.root(1) : Outcome.fallback(true);
    }

    /** Packages from {@code candidates} that are in the whitelist, in order. */
    public List<String> allowedOf(Collection<String> candidates) {
        List<String> out = new ArrayList<>();
        if (candidates != null) for (String p : candidates) if (allowed.contains(p) && !out.contains(p)) out.add(p);
        return out;
    }
}
