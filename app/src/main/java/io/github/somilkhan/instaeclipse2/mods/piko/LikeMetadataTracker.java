package io.github.somilkhan.instaeclipse2.mods.piko;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers what Instagram passed to the activation-metadata constructor before
 * {@link LikeAnimationHook} replaced it, so the models already built (and cached by Instagram)
 * follow the switch: turning Custom like animation off puts Instagram's original animation back,
 * and choosing another animation re-points them.
 *
 * <p>The hook passes {@link #MARK}, a Boolean instance of its own, as the "enabled" argument; the
 * model's two fields are then found by identity (the injected enum constant and MARK). If a
 * class doesn't store them as-is (e.g. unboxes the Boolean) it is not tracked.
 *
 * <p>No Xposed or Android dependencies, so it is safe to call from shared code and unit-testable.
 */
public final class LikeMetadataTracker {

    /** Current metadata constant to apply, or null when the feature is off. */
    public interface Picker {
        Object current();
    }

    /** Distinct from {@link Boolean#TRUE} so the field holding it can be found by identity. */
    @SuppressWarnings({"deprecation", "removal", "BooleanConstructorCall"})
    public static final Boolean MARK = new Boolean(true);

    static final int MAX_TRACKED = 4096;

    private static final class Entry {
        final WeakReference<Object> ref;
        final Field[] fields;
        final Object origEnum;
        final Object origEnabled;

        Entry(Object instance, Field[] fields, Object origEnum, Object origEnabled) {
            this.ref = new WeakReference<>(instance);
            this.fields = fields;
            this.origEnum = origEnum;
            this.origEnabled = origEnabled;
        }
    }

    private static final Field[] UNRESOLVED = new Field[0];
    private static final Map<Class<?>, Field[]> FIELDS = new ConcurrentHashMap<>();
    private static final ArrayDeque<Entry> TRACKED = new ArrayDeque<>();
    private static volatile Picker picker;

    private LikeMetadataTracker() {}

    public static void setPicker(Picker p) {
        picker = p;
    }

    /**
     * Records a constructed model whose arguments were replaced by {@code injected} and
     * {@link #MARK}. Returns false if its fields can't be identified (then it can't be restored).
     */
    public static boolean track(Object instance, Object injected, Object origEnum, Object origEnabled) {
        if (instance == null || injected == null) return false;
        Field[] f = FIELDS.get(instance.getClass());
        if (f == null) {
            f = resolve(instance, injected);
            FIELDS.put(instance.getClass(), f);
        }
        if (f == UNRESOLVED) return false;
        synchronized (TRACKED) {
            if (TRACKED.size() >= MAX_TRACKED) prune();
            if (TRACKED.size() >= MAX_TRACKED) TRACKED.pollFirst();
            TRACKED.addLast(new Entry(instance, f, origEnum, origEnabled));
        }
        return true;
    }

    /**
     * Brings tracked models in line with the flags: originals back when the picker returns null
     * (they are then forgotten), else the picker's current constant.
     */
    public static void onFlagsChanged() {
        Picker p = picker;
        Object value = null;
        if (p != null) {
            try {
                value = p.current();
            } catch (Throwable ignored) {
            }
        }
        synchronized (TRACKED) {
            for (Iterator<Entry> it = TRACKED.iterator(); it.hasNext(); ) {
                Entry e = it.next();
                Object inst = e.ref.get();
                if (inst == null) {
                    it.remove();
                    continue;
                }
                try {
                    if (value == null) {
                        e.fields[0].set(inst, e.origEnum);
                        e.fields[1].set(inst, e.origEnabled);
                        it.remove();
                    } else if (e.fields[0].get(inst) != value) {
                        e.fields[0].set(inst, value);
                        e.fields[1].set(inst, MARK);
                    }
                } catch (Throwable t) {
                    it.remove();
                }
            }
        }
    }

    static int trackedCount() {
        synchronized (TRACKED) {
            prune();
            return TRACKED.size();
        }
    }

    static void clearForTest() {
        synchronized (TRACKED) {
            TRACKED.clear();
        }
        FIELDS.clear();
        picker = null;
    }

    private static void prune() {
        TRACKED.removeIf(e -> e.ref.get() == null);
    }

    /** {enum field, enabled field}: the single instance field holding each injected value. */
    private static Field[] resolve(Object instance, Object injected) {
        Field enumField = null, enabledField = null;
        int enums = 0, marks = 0;
        for (Class<?> c = instance.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                Object v;
                try {
                    f.setAccessible(true);
                    v = f.get(instance);
                } catch (Throwable t) {
                    continue;
                }
                if (v == null) continue;
                if (v == injected) { enumField = f; enums++; }
                if (v == MARK) { enabledField = f; marks++; }
            }
        }
        if (enums != 1 || marks != 1) return UNRESOLVED;
        return new Field[] {enumField, enabledField};
    }
}
