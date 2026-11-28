package io.github.somilkhan.instaeclipse2.utils.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Features whose hooks are only installed when the flag is on at Instagram startup, so switching
 * them (on, at least) needs an Instagram restart. Both settings UIs (companion and in-IG dialog)
 * use {@link #withNote} so the label says so.
 */
public final class RestartRequired {

    public static final Set<String> KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "customThemeEnabled",   // IgThemeHook.install: no color hooks when off at start
            "customFontEnabled",    // CustomFontHook.install: typeface hooks only when on
            "customEmojiEnabled",   // CustomFontHook.install: typeface hooks only when on
            "disableVideoAutoPlay", // DisableVideoAutoPlayHook: overlay lifecycle hooks only when on
            "customStoryRingSize",  // StoryRingSizeHook.install returns when off
            "savedOnProfile",       // label already says so
            "expandCaptions")));    // label already says so

    private RestartRequired() {}

    public static boolean isRestartRequired(String key) {
        return key != null && KEYS.contains(key);
    }

    /**
     * {@code label} plus " (note)" for restart-required keys. Labels that already mention a
     * restart are returned unchanged, so the note is never doubled.
     */
    public static String withNote(String label, String key, String note) {
        if (label == null || note == null || note.isEmpty() || !isRestartRequired(key)) return label;
        if (label.toLowerCase(Locale.ROOT).contains("restart")
                || label.toLowerCase(Locale.ROOT).contains(note.toLowerCase(Locale.ROOT))) return label;
        return label + " (" + note + ")";
    }
}
