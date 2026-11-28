package io.github.somilkhan.instaeclipse2.utils.root;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RootActionsCrashTest {

    private final FakeShell shell = new FakeShell();
    private final RootSession session = new RootSession(shell, () -> true, s -> { }, () -> { });
    private final RootActions actions = new RootActions(session, 0, Arrays.asList("com.instagram.android"));
    private static final String DIR = "/data/user/0/com.instagram.android/files/eclipse2_crash_pending/";

    @Test public void savesThenDeletesOnlySavedReportsAndSkipsBadNames() {
        shell.answer = s -> {
            if (s.equals(RootCommands.checkScript())) return FakeShell.granted();
            if (s.contains("ls -1")) return FakeShell.ok("crash_a.txt\ncrash_b.txt\nevil'.txt\n");
            if (s.startsWith("head -c")) return FakeShell.ok(s.contains("crash_a") ? "report A" : "report B");
            return FakeShell.ok("");
        };
        List<String> saved = new ArrayList<>();
        RootActions.Outcome o = actions.collectCrashes("com.instagram.android", (name, text) -> {
            if (name.equals("crash_b")) return false; // folder not writable
            saved.add(name + "=" + text);
            return true;
        });
        assertEquals(RootCommands.Path.ROOT, o.path);
        assertEquals(1, o.count);
        assertFalse("one report was not saved", o.ok);
        assertEquals(Arrays.asList("crash_a=report A"), saved);
        assertTrue(shell.scripts.contains("rm -f '" + DIR + "crash_a.txt'"));
        assertFalse("unsaved report stays pending", shell.scripts.contains("rm -f '" + DIR + "crash_b.txt'"));
        for (String s : shell.scripts) assertFalse(s, s.contains("evil"));
    }

    @Test public void emptyDirIsOkWithZero() {
        shell.answer = s -> s.equals(RootCommands.checkScript()) ? FakeShell.granted() : FakeShell.ok("");
        RootActions.Outcome o = actions.collectCrashes("com.instagram.android", (n, t) -> true);
        assertEquals(RootCommands.Path.ROOT, o.path);
        assertTrue(o.ok);
        assertEquals(0, o.count);
    }

    @Test public void listingFailureFallsBack() {
        shell.queued.add(FakeShell.granted());
        shell.queued.add(FakeShell.fail(1, "Permission denied"));
        RootActions.Outcome o = actions.collectCrashes("com.instagram.android", (n, t) -> true);
        assertEquals(RootCommands.Path.FALLBACK, o.path);
        assertTrue(o.rootFailed);
    }

    @Test public void allowedOfFiltersToWhitelist() {
        assertEquals(Arrays.asList("com.instagram.android"),
                actions.allowedOf(Arrays.asList("com.evil", "com.instagram.android", "com.instagram.android")));
    }
}
