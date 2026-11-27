package io.github.somilkhan.instaeclipse2.mods.feed;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the labels in an obfuscated Kotlin data class's {@code toString()} (for example
 * {@code ", isShareEnabled="}) to the boolean fields they print.
 *
 * <p>Field names are obfuscated and change between Instagram builds. The label strings stay the
 * same. Reading the field order from the bytecode is not safe either: the compiler moves some
 * field reads out of print order (on 458.0.0.0.57 {@code isRepostButtonEnabled} is read near the
 * end of {@code toString}). So the mapping is measured on a real instance: flip each boolean
 * field once and see which label's printed value changes. Every original value is restored.
 *
 * <p>Pure Java (no Android, no Xposed), so it is unit tested on the JVM.
 */
public final class BooleanStateFields {

    private BooleanStateFields() {}

    /**
     * Resolves each label to the boolean instance field it prints.
     *
     * @return label → field for every label that maps to exactly one field. Labels that are
     *         missing from the output, or that more than one field changes, are left out, so a
     *         caller never writes a field it cannot identify.
     */
    public static Map<String, Field> resolve(Object state, List<String> labels) {
        Map<String, Field> out = new LinkedHashMap<>();
        if (state == null || labels == null || labels.isEmpty()) return out;
        String base = String.valueOf(state);
        List<String> wanted = new ArrayList<>();
        for (String label : labels) {
            if (valueAfter(base, label) != null) wanted.add(label);
        }
        if (wanted.isEmpty()) return out;

        Map<String, List<Field>> hits = new LinkedHashMap<>();
        for (String label : wanted) hits.put(label, new ArrayList<>());

        for (Field f : booleanFields(state.getClass())) {
            boolean original;
            try {
                original = f.getBoolean(state);
            } catch (Throwable t) {
                continue;
            }
            String flipped;
            try {
                f.setBoolean(state, !original);
                flipped = String.valueOf(state);
            } catch (Throwable t) {
                continue;
            } finally {
                try { f.setBoolean(state, original); } catch (Throwable ignored) {}
            }
            for (String label : wanted) {
                String before = valueAfter(base, label);
                String after = valueAfter(flipped, label);
                if (after != null && !after.equals(before)) hits.get(label).add(f);
            }
        }
        for (Map.Entry<String, List<Field>> e : hits.entrySet()) {
            if (e.getValue().size() == 1) out.put(e.getKey(), e.getValue().get(0));
        }
        return Collections.unmodifiableMap(out);
    }

    /** The value printed after {@code label}: "true", "false", or null if the label is absent. */
    static String valueAfter(String text, String label) {
        if (text == null || label == null) return null;
        int at = text.indexOf(label);
        if (at < 0) return null;
        int start = at + label.length();
        if (text.startsWith("true", start)) return "true";
        if (text.startsWith("false", start)) return "false";
        return null;
    }

    /** Declared non-static {@code boolean} fields, made accessible (final fields included). */
    static List<Field> booleanFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() != boolean.class || Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                out.add(f);
            } catch (Throwable ignored) {}
        }
        return out;
    }
}
