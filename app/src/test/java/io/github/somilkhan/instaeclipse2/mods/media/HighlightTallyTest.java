package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** MEDIA-07: highlight download-all reports only real outcomes. */
public class HighlightTallyTest {
    @Test public void skippedAndFailedItemsAreNotCountedAsSaved() {
        HighlightTally tally = new HighlightTally();
        tally.saved();
        tally.skipped();
        tally.failed();
        tally.saved();
        assertEquals(2, tally.savedCount());
        assertEquals(0, tally.queuedCount());
        assertEquals(1, tally.skippedCount());
        assertEquals(1, tally.failedCount());
        assertEquals(2, tally.handled());
    }

    @Test public void companionHandOffsAreCountedSeparately() {
        HighlightTally tally = new HighlightTally();
        tally.queued();
        tally.queued();
        tally.failed();
        assertEquals(0, tally.savedCount());
        assertEquals(2, tally.queuedCount());
        assertEquals(2, tally.handled());
        assertEquals("saved=0 queued=2 skipped=0 failed=1", tally.toString());
    }
}
