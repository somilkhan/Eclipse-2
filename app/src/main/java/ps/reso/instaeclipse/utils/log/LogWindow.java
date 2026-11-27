package ps.reso.instaeclipse.utils.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, incrementally updated view of a log. The underlying files stay complete; only the
 * on-screen list is capped so a long session cannot freeze the UI.
 *
 * <p>{@link #update(List)} is computed off the main thread: given a fresh full read it decides
 * whether the new list is the old one plus appended items (cheap range insert on screen) or a
 * different list (full replace).
 */
public final class LogWindow {

    private LogWindow() {}

    /** The last {@code max} items of {@code all} (a copy). */
    public static <T> List<T> tail(List<T> all, int max) {
        if (max <= 0) return new ArrayList<>();
        if (all.size() <= max) return new ArrayList<>(all);
        return new ArrayList<>(all.subList(all.size() - max, all.size()));
    }

    /** Result of comparing the currently shown list with a fresh read. */
    public static final class Delta<T> {
        /** True when {@link #appended} should be added to the end of the current list. */
        public final boolean incremental;
        /** New tail items (incremental) or the complete new list (replace). */
        public final List<T> items;
        /** How many items to drop from the front after appending, to stay within the bound. */
        public final int dropFromFront;

        Delta(boolean incremental, List<T> items, int dropFromFront) {
            this.incremental = incremental;
            this.items = items;
            this.dropFromFront = dropFromFront;
        }

        public boolean isEmpty() {
            return incremental && items.isEmpty();
        }
    }

    /**
     * Compares {@code current} (what is on screen) with {@code fresh} (complete new read) and
     * returns the cheapest correct update. Items are compared with {@code equals}.
     */
    public static <T> Delta<T> diff(List<T> current, List<T> fresh, int max) {
        List<T> bounded = tail(fresh, max);
        if (current.isEmpty()) return new Delta<>(false, bounded, 0);
        T last = current.get(current.size() - 1);
        // Find the newest occurrence of the old last item in the fresh list.
        int idx = -1;
        for (int i = fresh.size() - 1; i >= 0; i--) {
            if (Objects.equals(fresh.get(i), last)) { idx = i; break; }
        }
        if (idx < 0) return new Delta<>(false, bounded, 0);
        // Everything before it that is still on screen must match, otherwise replace.
        int overlap = Math.min(current.size(), idx + 1);
        for (int k = 1; k <= overlap; k++) {
            if (!Objects.equals(current.get(current.size() - k), fresh.get(idx + 1 - k))) {
                return new Delta<>(false, bounded, 0);
            }
        }
        List<T> appended = fresh.subList(idx + 1, fresh.size());
        if (appended.size() >= max) return new Delta<>(false, bounded, 0);
        int total = current.size() + appended.size();
        int drop = Math.max(0, total - max);
        return new Delta<>(true, Collections.unmodifiableList(new ArrayList<>(appended)), drop);
    }
}
