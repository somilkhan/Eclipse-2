package io.github.somilkhan.instaeclipse2.utils.backup;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import io.github.somilkhan.instaeclipse2.mods.ads.ShareDomain;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

/**
 * Instagram-side settings backup / restore.
 *
 * <p>The key list is not maintained by hand: every public static, non-final boolean / int /
 * String / double field of {@link FeatureFlags} is a setting (the same schema
 * {@code ModuleReplies.preferenceTypes} and {@code SettingsManager} use), minus
 * {@link SettingsRestore#NEVER_RESTORED}. New flags are backed up without touching this file.
 *
 * <p>Restore is two-phase: the whole file is parsed and type-checked by
 * {@link SettingsRestore#parse} (unknown / mistyped keys are skipped, never thrown on), values
 * are sanitised into a staged map, and only then assigned. A bad value can't leave a half-applied
 * restore behind.
 */
public class SettingsBackupManager {

    private static final int VERSION = 1;

    /** Backed-up settings: name to field, in name order. */
    static Map<String, Field> backupFields() {
        Map<String, Field> out = new TreeMap<>();
        for (Field f : FeatureFlags.class.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod) || Modifier.isFinal(mod)) continue;
            Class<?> t = f.getType();
            if (t != boolean.class && t != int.class && t != String.class && t != double.class) continue;
            if (SettingsRestore.NEVER_RESTORED.contains(f.getName())) continue;
            out.put(f.getName(), f);
        }
        return Collections.unmodifiableMap(out);
    }

    /** Serialises every backed-up FeatureFlag into a versioned JSON string. */
    public static String toJson() throws JSONException {
        JSONObject s = new JSONObject();
        for (Map.Entry<String, Field> e : backupFields().entrySet()) {
            Object v;
            try {
                v = e.getValue().get(null);
            } catch (IllegalAccessException unreachable) {
                continue;
            }
            // SettingsManager persists double-valued flags (spoofLat/spoofLng) as strings.
            if (v instanceof Double) v = String.valueOf(v);
            s.put(e.getKey(), v == null ? "" : v);
        }
        JSONObject root = new JSONObject();
        root.put("version", VERSION);
        root.put("settings", s);
        return root.toString(2);
    }

    /**
     * Applies a backup JSON string to the in-memory FeatureFlags (call
     * {@code SettingsManager.saveAllFlags()} afterwards to persist). Accepts the versioned
     * {@code {"version":1,"settings":{...}}} format and a flat {@code {key:value}} object.
     *
     * @return number of settings applied
     * @throws IllegalArgumentException when the text is not a JSON object; nothing is changed then
     */
    public static int fromJson(String json) {
        Map<String, Object> parsed = SettingsRestore.parse(json);
        boolean locked = SettingsRestore.isLocked(FeatureFlags.isExtremeMode, FeatureFlags.focusLockUntil,
                System.currentTimeMillis());
        Map<String, Object> staged = stage(SettingsRestore.filterLocked(parsed, locked));
        apply(staged);
        return staged.size();
    }

    /**
     * Phase 1: converts and sanitises every value; returns field-ready values keyed by name.
     * Pure (no FeatureFlags writes).
     */
    static Map<String, Object> stage(Map<String, Object> parsed) {
        Map<String, Field> fields = backupFields();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : parsed.entrySet()) {
            String key = e.getKey();
            Field f = fields.get(key);
            if (f == null) continue;
            Object v = sanitize(key, f.getType(), e.getValue());
            if (v != null) out.put(key, v);
        }
        return out;
    }

    private static Object sanitize(String key, Class<?> type, Object v) {
        if (type == boolean.class) return v instanceof Boolean ? v : null;
        if (type == int.class) return v instanceof Integer ? v : null;
        if (type == double.class) {
            if (!(v instanceof String)) return null;
            try {
                double d = Double.parseDouble(((String) v).trim());
                return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
            } catch (NumberFormatException bad) {
                return null;
            }
        }
        if (type != String.class || !(v instanceof String)) return null;
        String s = (String) v;
        switch (key) {
            case "startupTab":
                return Arrays.asList(io.github.somilkhan.instaeclipse2.mods.ui.StartupTabHook.TABS).contains(s) ? s : null;
            case "customShareDomain":
                return ShareDomain.normalize(s);
            case "exactTimestampFormat":
                return io.github.somilkhan.instaeclipse2.utils.text.TimestampFormat.isValid(s) ? s : null;
            default:
                return s;
        }
    }

    /** Phase 2: assigns the staged values. Types were checked in {@link #stage}. */
    private static void apply(Map<String, Object> staged) {
        Map<String, Field> fields = backupFields();
        for (Map.Entry<String, Object> e : staged.entrySet()) {
            try {
                fields.get(e.getKey()).set(null, e.getValue());
            } catch (IllegalAccessException | IllegalArgumentException unreachable) {
                // stage() only returns values of the field's type
            }
        }
    }
}
