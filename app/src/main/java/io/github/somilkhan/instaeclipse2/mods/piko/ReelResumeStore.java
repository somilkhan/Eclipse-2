package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.LinkedHashMap;
import java.util.Map;

/** Last playback position per reel, bounded LRU (pure, unit tested). */
public final class ReelResumeStore {

    /** Positions under this are not worth resuming (a reel barely started). */
    public static final int MIN_POSITION_MS = 1000;

    private final Map<String, Integer> positions;

    public ReelResumeStore(int capacity) {
        final int cap = Math.max(1, capacity);
        positions = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> e) {
                return size() > cap;
            }
        };
    }

    public synchronized void save(String id, int positionMs) {
        if (id == null || id.isEmpty()) return;
        if (positionMs < MIN_POSITION_MS) positions.remove(id);
        else positions.put(id, positionMs);
    }

    /** The saved position (and forgets it), or -1. */
    public synchronized int take(String id) {
        if (id == null) return -1;
        Integer v = positions.remove(id);
        return v == null ? -1 : v;
    }

    public synchronized int size() {
        return positions.size();
    }
}
