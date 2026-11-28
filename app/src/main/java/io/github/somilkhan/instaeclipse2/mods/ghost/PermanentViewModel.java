package io.github.somilkhan.instaeclipse2.mods.ghost;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure reflection logic behind {@link GhostPermanentViewHook}: locating the parsed model's
 * {@code seen_count} field and rewriting its {@code view_mode} string. Kept free of Xposed and
 * Android so it can be unit-tested against fake model classes.
 */
final class PermanentViewModel {

    /** How the seen-count field of a model class was resolved. */
    static final class SeenCount {
        /** The field, or null if none could be chosen. */
        final Field field;
        /** Why {@link #field} is null ("none" / "ambiguous: a,b,c"); null when resolved. */
        final String unresolvedReason;

        SeenCount(Field field, String unresolvedReason) {
            this.field = field;
            this.unresolvedReason = unresolvedReason;
        }
    }

    private static final Map<Class<?>, SeenCount> SEEN_COUNT_BY_CLASS = new ConcurrentHashMap<>();

    private PermanentViewModel() {}

    /**
     * Picks the {@code seen_count} field of {@code cls}, cached per class. Only instance
     * {@code int} fields in the class hierarchy are considered. A field named
     * {@code seen_count}/{@code seenCount} wins; otherwise a single candidate is used. With
     * several unnamed candidates (obfuscated model with e.g. width/height ints) nothing is chosen:
     * guessing would make the conversion arbitrary.
     */
    static SeenCount resolveSeenCount(Class<?> cls) {
        SeenCount cached = SEEN_COUNT_BY_CLASS.get(cls);
        if (cached != null) return cached;
        List<Field> candidates = new ArrayList<>();
        Field named = null;
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != int.class || Modifier.isStatic(f.getModifiers())) continue;
                candidates.add(f);
                String n = f.getName().replace("_", "");
                if (named == null && n.equalsIgnoreCase("seencount")) named = f;
            }
        }
        SeenCount res;
        if (named != null) {
            res = new SeenCount(named, null);
        } else if (candidates.size() == 1) {
            res = new SeenCount(candidates.get(0), null);
        } else if (candidates.isEmpty()) {
            res = new SeenCount(null, "none");
        } else {
            StringBuilder sb = new StringBuilder("ambiguous:");
            for (Field f : candidates) sb.append(' ').append(f.getName());
            res = new SeenCount(null, sb.toString());
        }
        if (res.field != null) {
            try {
                res.field.setAccessible(true);
            } catch (Throwable t) {
                res = new SeenCount(null, "inaccessible: " + res.field.getName());
            }
        }
        SEEN_COUNT_BY_CLASS.put(cls, res);
        return res;
    }

    /** Seen count of {@code model}, or -1 if its field is unresolved/unreadable. */
    static int readSeenCount(Object model, SeenCount sc) {
        if (sc.field == null) return -1;
        try {
            return sc.field.getInt(model);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Original ephemeral type in {@code model}'s instance String fields: "once" if any field is
     * "once", else "replayable" for "replayable"/"allow_replay", else null.
     */
    static String originalMode(Object model) {
        String found = null;
        for (Field f : viewModeFields(model)) {
            String v = readString(f, model);
            if ("once".equals(v)) return "once";
            if (isReplayable(v)) found = "replayable";
        }
        return found;
    }

    /**
     * True if media of {@code original} type has used up its views. An unknown seen count
     * ({@code < 0}) does not block the conversion.
     */
    static boolean consumed(String original, int seenCount) {
        if (seenCount < 0) return false;
        if ("once".equals(original)) return seenCount >= 1;
        if ("replayable".equals(original)) return seenCount >= 2;
        return false;
    }

    /** Rewrites every instance view_mode String field of {@code model} to {@code value}. */
    static int rewriteViewModes(Object model, String value) {
        int n = 0;
        for (Field f : viewModeFields(model)) {
            String v = readString(f, model);
            if (!"once".equals(v) && !isReplayable(v)) continue;
            try {
                f.set(model, value);
                n++;
            } catch (Throwable ignored) {
            }
        }
        return n;
    }

    private static boolean isReplayable(String v) {
        return "replayable".equals(v) || "allow_replay".equals(v);
    }

    private static List<Field> viewModeFields(Object model) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class || Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    out.add(f);
                } catch (Throwable ignored) {
                }
            }
        }
        return out;
    }

    private static String readString(Field f, Object model) {
        try {
            return (String) f.get(model);
        } catch (Throwable t) {
            return null;
        }
    }
}
