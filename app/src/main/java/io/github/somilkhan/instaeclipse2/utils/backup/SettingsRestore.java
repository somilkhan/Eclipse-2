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
            // A timed focus lock must not come back (or be cut short) via an old backup.
            "focusLockUntil")));

    private SettingsRestore() {}

    /**
     * @return accepted settings in file order (Boolean, Integer or String values)
     * @throws IllegalArgumentException when the text is not a JSON object
     */
    public static Map<String, Object> parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("Not a settings backup", malformed);
        }
        if (root == null || !root.isJsonObject()) throw new IllegalArgumentException("Not a settings backup");
        JsonObject obj = root.getAsJsonObject();
        JsonObject settings = obj.has("settings") && obj.get("settings").isJsonObject()
                ? obj.getAsJsonObject("settings") : obj;

        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : settings.entrySet()) {
            String key = e.getKey();
            if (NEVER_RESTORED.contains(key) || !e.getValue().isJsonPrimitive()) continue;
            Object value = typed(key, e.getValue().getAsJsonPrimitive());
            if (value != null) out.put(key, value);
        }
        return out;
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
