package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The newest read receipt Ghost seen held back, per thread, so it can be sent once the user
 * replies there. Bounded (oldest thread dropped first); thread-safe. Pure, unit tested.
 */
public final class PendingReceipts<T> {

    public static final int MAX_THREADS = 64;

    private final int max;
    private final LinkedHashMap<String, T> byThread;

    public PendingReceipts() { this(MAX_THREADS); }

    public PendingReceipts(int max) {
        this.max = Math.max(1, max);
        this.byThread = new LinkedHashMap<>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, T> e) { return size() > PendingReceipts.this.max; }
        };
    }

    /** Remembers the receipt for a thread; a newer one replaces the older (it covers it). */
    public synchronized void hold(String threadId, T receipt) {
        if (threadId == null || threadId.isEmpty() || receipt == null) return;
        byThread.put(threadId, receipt);
    }

    /** Removes and returns the held receipt for a thread, or null. */
    public synchronized T release(String threadId) {
        return threadId == null ? null : byThread.remove(threadId);
    }

    public synchronized int size() { return byThread.size(); }

    public synchronized void clear() { byThread.clear(); }
}
