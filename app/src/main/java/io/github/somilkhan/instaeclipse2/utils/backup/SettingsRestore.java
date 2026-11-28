package io.github.somilkhan.instaeclipse2.utils.backup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies;

/**
 * Companion-side parsing of a settings backup file ({@link SettingsBackupManager#toJson} format,
 * {@code {"version":1,"settings":{...}}}, or a flat {@code {key: value}} object).
 *
 * <p>Only known settings with the right type are returned (see
 * {@link ModuleReplies#isPreferenceValue}); device-specific or secret keys are never restored.
 * Integer settings stored as JSON numbers and string settings (e.g. spoofLat) are kept, which the
 * old Boolean-only restore silently dropped.
 */
public final class SettingsRestore {

    /** Keys that must not travel between installs/devices through a backup file. */
    static final Set<String> NEVER_RESTORED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // Passcode hash and salt stay inside Instagram's process.
            "lockDirectPasscode", "lockDirectSalt",
            // SAF grant and file paths belong to the device/install that made the backup.
            "downloaderCustomUri", "downloaderCustomPath", "customFontPath", "customEmojiPath",
            "crashReportUri", "crashReportPath",
            // A timed focus lock must not come back (or be cut short) via an old backup. Extreme
            // Mode itself is only ever turned on through its confirm dialog and only ever turned
            // off by reinstalling or a finished Focus Lock: a backup must not do either (a restored
            // "true" without focusLockUntil would also turn a timed lock into a permanent one).
            "focusLockUntil", "isExtremeMode", "isDistractionFree",
            // Derived from the ghost toggles on every save (FeatureFlags.ghostMasterState).
            "isGhostModeEnabled")));

    /** Distraction switches Extreme Mode locks; a restore must not change them while it is on. */
    public static final Set<String> LOCKED_WHILE_EXTREME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "disableStories", "disableFeed", "disableReels", "disableReelsExceptDM",
            "disableExplore", "disableComments")));

    /**
     * Companion-only preferences (not FeatureFlags fields, never sent to Instagram) that a
     * companion backup carries, with their stored type.
     */
    public static final Map<String, Class<?>> COMPANION_LOCAL;
    static {
        Map<String, Class<?>> m = new LinkedHashMap<>();
        m.put("crashReports", Boolean.class);        // CrashReportProvider.PREF_ENABLED
        m.put("homePinnedShortcuts", String.class);  // HomeFragment pinned toggles
        COMPANION_LOCAL = Collections.unmodifiableMap(m);
    }

    private SettingsRestore() {}

    /**
     * @return accepted settings in file order (Boolean, Integer or String values)
     * @throws IllegalArgumentException when the text is not a JSON object
     */
    public static Map<String, Object> parse(String json) {
        JsonObject settings = settingsObject(json);
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : settings.entrySet()) {
            String key = e.getKey();
            if (NEVER_RESTORED.contains(key) || !e.getValue().isJsonPrimitive()) continue;
            Object value = typed(key, e.getValue().getAsJsonPrimitive());
            if (value != null) out.put(key, value);
        }
        return out;
    }

    /**
     * Companion-only settings in the backup (see {@link #COMPANION_LOCAL}), typed. They are written
     * to the companion's own prefs and never broadcast.
     */
    public static Map<String, Object> parseCompanionLocal(String json) {
        JsonObject settings = settingsObject(json);
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Class<?>> e : COMPANION_LOCAL.entrySet()) {
            JsonElement v = settings.get(e.getKey());
            if (v == null || !v.isJsonPrimitive()) continue;
            JsonPrimitive p = v.getAsJsonPrimitive();
            if (e.getValue() == Boolean.class && p.isBoolean()) out.put(e.getKey(), p.getAsBoolean());
            else if (e.getValue() == String.class && p.isString()
                    && p.getAsString().length() <= 16 * 1024) out.put(e.getKey(), p.getAsString());
        }
        return out;
    }

    /**
     * True while Extreme Mode holds the distraction switches: it is on and no Focus Lock end time
     * has passed (an expired lock is released on the next load anyway).
     */
    public static boolean isLocked(boolean extremeMode, String focusLockUntil, long now) {
        return extremeMode && !io.github.somilkhan.instaeclipse2.utils.feature.FocusLock.isExpired(focusLockUntil, now);
    }

    /** {@code restored} without the Extreme-Mode-locked keys when {@code locked}; a new map either way. */
    public static Map<String, Object> filterLocked(Map<String, Object> restored, boolean locked) {
        Map<String, Object> out = new LinkedHashMap<>(restored);
        if (locked) out.keySet().removeAll(LOCKED_WHILE_EXTREME);
        return out;
    }

    /**
     * What a companion backup file should contain from its prefs: every known, correctly typed
     * setting (Boolean, Integer and String, not only Booleans) plus {@link #COMPANION_LOCAL},
     * without sync stamps ({@code __...}) and without {@link #NEVER_RESTORED} keys.
     */
    public static Map<String, Object> backupEntries(Map<String, ?> prefs) {
        Map<String, Object> out = new java.util.TreeMap<>();
        for (Map.Entry<String, ?> e : prefs.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if (key == null || v == null || key.startsWith("__") || NEVER_RESTORED.contains(key)) continue;
            Class<?> local = COMPANION_LOCAL.get(key);
            if ((local != null && local.isInstance(v)) || ModuleReplies.isPreferenceValue(key, v)) out.put(key, v);
        }
        return out;
    }

    private static JsonObject settingsObject(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("Not a settings backup", malformed);
        }
        if (root == null || !root.isJsonObject()) throw new IllegalArgumentException("Not a settings backup");
        JsonObject obj = root.getAsJsonObject();
        return obj.has("settings") && obj.get("settings").isJsonObject() ? obj.getAsJsonObject("settings") : obj;
    }

    /** The value converted to the type the setting is stored as, or null when it doesn't fit. */
    private static Object typed(String key, JsonPrimitive p) {
        if (p.isBoolean()) {
            Boolean b = p.getAsBoolean();
            return ModuleReplies.isPreferenceValue(key, b) ? b : null;
        }
        if (p.isNumber()) {
            try {
                Integer i = new BigDecimal(p.getAsString()).intValueExact();
                if (ModuleReplies.isPreferenceValue(key, i)) return i;
            } catch (ArithmeticException | NumberFormatException notAnInt) { /* try as string */ }
            // Double-valued settings (spoofLat/spoofLng) are persisted as strings.
            String s = p.getAsString();
            return ModuleReplies.isPreferenceValue(key, s) ? s : null;
        }
        String s = p.getAsString();
        return ModuleReplies.isPreferenceValue(key, s) ? s : null;
    }

    /** Broadcast action that carries a value of this type to Instagram. */
    public static String actionFor(Object value) {
        String base = "io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF";
        if (value instanceof Integer) return base + "_INT";
        if (value instanceof String) return base + "_STRING";
        return base;
    }
}
