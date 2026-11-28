package io.github.somilkhan.instaeclipse2.utils.root;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class RootSessionTest {

    private final FakeShell shell = new FakeShell();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final List<String> log = new ArrayList<>();
    private final RootSession session = new RootSession(shell, enabled::get, log::add, () -> { });
    private final RootActions actions = new RootActions(session, 0, Arrays.asList("com.instagram.android"));

    @Test public void offByDefaultNeverStartsSu() {
        enabled.set(false);
        assertNull(session.run("x", "id", 1000));
        assertEquals(RootCommands.Path.FALLBACK, actions.forceStop("com.instagram.android").path);
        assertFalse(actions.forceStop("com.instagram.android").rootFailed);
        assertNull(actions.readModuleLog("com.instagram.android"));
        assertTrue(shell.scripts.isEmpty());
    }

    @Test public void firstUseChecksThenRunsAndCaches() {
        shell.queued.add(FakeShell.granted());
        assertEquals(RootCommands.Path.ROOT, actions.forceStop("com.instagram.android").path);
        assertEquals(RootStatus.GRANTED, session.status());
        assertEquals(Arrays.asList(RootCommands.checkScript(), "am force-stop --user 0 'com.instagram.android'"), shell.scripts);
        actions.forceStop("com.instagram.android");
        assertEquals("check is cached", 3, shell.scripts.size());
    }

    @Test public void noSuMeansNotRootedAndFallbackWithoutRetrying() {
        shell.answer = s -> FakeShell.noSu();
        RootActions.Outcome o = actions.forceStop("com.instagram.android");
        assertEquals(RootCommands.Path.FALLBACK, o.path);
        assertEquals(RootStatus.NOT_ROOTED, session.status());
        actions.forceStop("com.instagram.android");
        assertEquals("not retried once known", 1, shell.scripts.size());
        assertTrue(log.toString(), log.stream().anyMatch(l -> l.contains("NOT_ROOTED")));
    }

    @Test public void denialAndTimeoutAreDeniedAndLogged() {
        shell.answer = s -> FakeShell.fail(1, "permission denied");
        assertEquals(RootStatus.DENIED, session.check());
        assertTrue(log.toString(), log.stream().anyMatch(l -> l.contains("DENIED") && l.contains("permission denied")));
        session.reset();
        shell.answer = s -> FakeShell.timeout();
        assertEquals(RootStatus.DENIED, session.check());
        assertTrue(log.toString(), log.stream().anyMatch(l -> l.contains("timed out")));
    }

    @Test public void failedCommandFallsBackAndSaysRootFailed() {
        shell.queued.add(FakeShell.granted());
        shell.queued.add(FakeShell.fail(255, "Unknown package"));
        RootActions.Outcome o = actions.clearDexKitCache("com.instagram.android");
        assertEquals(RootCommands.Path.FALLBACK, o.path);
        assertTrue(o.rootFailed);
        assertEquals("a failing command does not revoke the grant", RootStatus.GRANTED, session.status());
        assertTrue(log.toString(), log.stream().anyMatch(l -> l.contains("clear DexKit cache") && l.contains("exit 255")));
    }

    @Test public void reEnableAfterDenialChecksAgain() {
        shell.queued.add(FakeShell.fail(1, ""));
        assertEquals(RootStatus.DENIED, session.check());
        assertNull(session.run("x", "id", 1000));
        session.reset(); // RootAccess.setEnabled / "Check again"
        shell.queued.add(FakeShell.granted());
        assertEquals(RootStatus.GRANTED, session.check());
    }

    @Test public void checkSkippedWhenOff() {
        enabled.set(false);
        assertEquals(RootStatus.UNKNOWN, session.check());
        assertTrue(shell.scripts.isEmpty());
    }

    @Test public void mainThreadGuardRunsBeforeAnything() {
        RootSession guarded = new RootSession(shell, () -> true, log::add,
                () -> { throw new IllegalStateException("main"); });
        try {
            guarded.run("x", "id", 1);
            fail();
        } catch (IllegalStateException expected) { }
        assertTrue(shell.scripts.isEmpty());
    }

    @Test public void readModuleLogReturnsOutputOnlyOnSuccess() {
        shell.queued.add(FakeShell.granted());
        shell.queued.add(FakeShell.ok("2026-11-28 10:00:00.000 hooked\n"));
        assertEquals("2026-11-28 10:00:00.000 hooked\n", actions.readModuleLog("com.instagram.android"));
        shell.queued.add(FakeShell.fail(1, "No such file"));
        assertNull(actions.readModuleLog("com.instagram.android"));
    }
}
