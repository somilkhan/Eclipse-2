package io.github.somilkhan.instaeclipse2.utils.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogWindowTest {

    private static List<String> range(int from, int to) {
        List<String> l = new ArrayList<>();
        for (int i = from; i < to; i++) l.add("e" + i);
        return l;
    }

    @Test
    public void tailKeepsNewestItems() {
        assertEquals(range(7, 10), LogWindow.tail(range(0, 10), 3));
        assertEquals(range(0, 2), LogWindow.tail(range(0, 2), 3));
        assertTrue(LogWindow.tail(range(0, 2), 0).isEmpty());
    }

    @Test
    public void firstLoadReplacesWithBoundedList() {
        LogWindow.Delta<String> d = LogWindow.diff(Collections.emptyList(), range(0, 10), 4);
        assertFalse(d.incremental);
        assertEquals(range(6, 10), d.items);
    }

    @Test
    public void appendedEntriesAreIncremental() {
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 5), range(0, 8), 100);
        assertTrue(d.incremental);
        assertEquals(range(5, 8), d.items);
        assertEquals(0, d.dropFromFront);
    }

    @Test
    public void unchangedIsEmptyIncrementalDelta() {
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 5), range(0, 5), 100);
        assertTrue(d.isEmpty());
    }

    @Test
    public void boundedViewDropsFromFrontWhileAppending() {
        // On screen: last 4 of e0..e9. File grew to e0..e11 (complete); view stays at 4.
        LogWindow.Delta<String> d = LogWindow.diff(range(6, 10), range(0, 12), 4);
        assertTrue(d.incremental);
        assertEquals(range(10, 12), d.items);
        assertEquals(2, d.dropFromFront);
    }

    @Test
    public void persistedFileRotatedFrontStillIncremental() {
        // Logging dropped the oldest lines from the file; the overlap still matches.
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 6), range(3, 9), 100);
        assertTrue(d.incremental);
        assertEquals(range(6, 9), d.items);
    }

    @Test
    public void editedLastEntryForcesReplace() {
        // Repeats are collapsed in place ("msg (×2)"), so the last entry changes.
        List<String> fresh = new ArrayList<>(range(0, 4));
        fresh.add("e4 (x2)");
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 5), fresh, 100);
        assertFalse(d.incremental);
        assertEquals(fresh, d.items);
    }

    @Test
    public void clearedLogForcesReplace() {
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 5), Arrays.asList("new"), 100);
        assertFalse(d.incremental);
        assertEquals(Arrays.asList("new"), d.items);
    }

    @Test
    public void mismatchedHistoryForcesReplace() {
        LogWindow.Delta<String> d = LogWindow.diff(Arrays.asList("a", "x"), Arrays.asList("b", "x", "y"), 100);
        assertFalse(d.incremental);
    }

    @Test
    public void hugeAppendFallsBackToReplace() {
        LogWindow.Delta<String> d = LogWindow.diff(range(0, 2), range(0, 50), 10);
        assertFalse(d.incremental);
        assertEquals(range(40, 50), d.items);
    }
}
