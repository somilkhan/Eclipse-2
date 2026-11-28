package io.github.somilkhan.instaeclipse2.mods.media;

/**
 * Outcome counts for highlight "download all". Only finished in-process saves count as saved;
 * companion hand-offs are counted separately because that service reports its own result, and
 * items without media or with a failed save are never reported as done.
 */
final class HighlightTally {
    private int saved, queued, skipped, failed;

    void saved() { saved++; }
    void queued() { queued++; }
    void skipped() { skipped++; }
    void failed() { failed++; }

    int savedCount() { return saved; }
    int queuedCount() { return queued; }
    int skippedCount() { return skipped; }
    int failedCount() { return failed; }

    /** Items that were saved here or accepted by the companion service. */
    int handled() { return saved + queued; }

    @Override public String toString() {
        return "saved=" + saved + " queued=" + queued + " skipped=" + skipped + " failed=" + failed;
    }
}
