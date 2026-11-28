package io.github.somilkhan.instaeclipse2.utils.root;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Root state for one process: the user's opt-in, the cached check result and the gate every root
 * command goes through. Pure Java; {@link RootAccess} wires it to {@link SuShellRunner}, the
 * settings and the log. Blocking methods must run off the main thread ({@code offMainCheck}).
 *
 * <p>Nothing here is reachable from another app: it is plain in-process code behind no exported
 * component, and only {@link RootCommands} builds the scripts.
 */
public class RootSession {

    /** Long enough for the user to answer the root manager's prompt (Magisk's default is 10 s). */
    public static final long CHECK_TIMEOUT_MS = 30_000;
    public static final long ACTION_TIMEOUT_MS = 20_000;

    private final ShellRunner runner;
    private final BooleanSupplier enabled;
    private final Consumer<String> log;
    private final Runnable offMainCheck;
    private final Object checkLock = new Object();
    private volatile RootStatus status = RootStatus.UNKNOWN;

    public RootSession(ShellRunner runner, BooleanSupplier enabled, Consumer<String> log, Runnable offMainCheck) {
        this.runner = runner;
        this.enabled = enabled;
        this.log = log;
        this.offMainCheck = offMainCheck;
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    /** Cached result; never runs su. */
    public RootStatus status() {
        return status;
    }

    /** Forget the cached result (the user asked to check again, or switched root off). */
    public void reset() {
        status = RootStatus.UNKNOWN;
    }

    /** Which way the next action will go, without running anything. */
    public RootCommands.Path plannedPath() {
        return RootCommands.choose(isEnabled(), status);
    }

    /**
     * Runs the check (may show the root manager's prompt) and caches the result. Concurrent
     * callers share one check. Does nothing but log when root is switched off.
     */
    public RootStatus check() {
        offMainCheck.run();
        if (!isEnabled()) {
            log.accept("(Root) check skipped: \"Use root\" is off");
            return status;
        }
        synchronized (checkLock) {
            ShellRunner.Result r = runner.run(RootCommands.checkScript(), CHECK_TIMEOUT_MS);
            RootStatus s;
            if (r.noSu) s = RootStatus.NOT_ROOTED;
            else if (RootCommands.isGranted(r.exitCode, r.stdout)) s = RootStatus.GRANTED;
            else s = RootStatus.DENIED;
            status = s;
            if (s == RootStatus.GRANTED) log.accept("(Root) access granted");
            else log.accept("(Root) check: " + s + " - " + r.summary());
            return s;
        }
    }

    /**
     * Runs {@code script} as root when the user opted in and access is (or, on first use, turns
     * out to be) granted. Returns {@code null} when the caller must use its non-root fallback;
     * a non-null result may still have failed (check {@link ShellRunner.Result#ok()}). Every
     * failure is logged with {@code what}.
     */
    public ShellRunner.Result run(String what, String script, long timeoutMs) {
        offMainCheck.run();
        if (RootCommands.choose(isEnabled(), status) == RootCommands.Path.FALLBACK) {
            if (isEnabled()) log.accept("(Root) " + what + ": not used (" + status + ")");
            return null;
        }
        if (status != RootStatus.GRANTED && check() != RootStatus.GRANTED) {
            log.accept("(Root) " + what + ": falling back, root " + status);
            return null;
        }
        ShellRunner.Result r = runner.run(script, timeoutMs);
        if (r.noSu) {
            status = RootStatus.NOT_ROOTED;
            log.accept("(Root) " + what + " failed: " + r.summary());
        } else if (!r.ok()) {
            log.accept("(Root) " + what + " failed: " + r.summary());
        }
        return r;
    }
}
