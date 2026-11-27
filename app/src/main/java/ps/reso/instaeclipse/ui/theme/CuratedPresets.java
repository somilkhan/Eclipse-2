package ps.reso.instaeclipse.ui.theme;

import java.util.ArrayList;
import java.util.List;

import ps.reso.instaeclipse.mods.ui.theme.ThemePreset;
import ps.reso.instaeclipse.mods.ui.theme.ThemePresets;

/**
 * Companion-side curation of the in-Instagram theme presets. The module still knows every preset
 * (saved ids keep working); the companion only offers a short, restrained set — near-neutral
 * surfaces with one accent — plus whatever preset the user already has selected.
 */
public final class CuratedPresets {

    /** Base preset ids (id % VARIANT_OFFSET), in display order. */
    static final int[] BASE_IDS = {
            2,  // Pure OLED
            1,  // Midnight Eclipse
            14, // Slate Pro
            21, // Steel Gray
            29, // Carbon Fiber
            5,  // Ocean Deep
            16, // Nordic Ice
            3,  // Arctic Frost
    };

    private CuratedPresets() {}

    public static boolean isCurated(int id) {
        if (id <= 0) return false;
        int base = id % ThemePresets.VARIANT_OFFSET;
        for (int b : BASE_IDS) if (b == base) return true;
        return false;
    }

    /** Curated presets for one mode, in curated order; {@code keepId} is appended if not already shown. */
    public static List<ThemePreset> forMode(boolean light, int keepId) {
        List<ThemePreset> out = new ArrayList<>();
        for (int base : BASE_IDS) {
            for (int id : new int[]{base, ThemePresets.VARIANT_OFFSET + base}) {
                if (ThemePresets.isLightPreset(id) == light) out.add(ThemePresets.getById(id));
            }
        }
        if (keepId > 0 && !isCurated(keepId) && ThemePresets.isLightPreset(keepId) == light) {
            out.add(ThemePresets.getById(keepId));
        }
        return out;
    }

    /** Filters picker values (non-positive sentinels such as "custom"/"stock" are always kept). */
    public static int[] filter(int[] values, int keepId) {
        List<Integer> kept = new ArrayList<>();
        for (int v : values) if (v <= 0 || isCurated(v) || v == keepId) kept.add(v);
        int[] out = new int[kept.size()];
        for (int i = 0; i < out.length; i++) out[i] = kept.get(i);
        return out;
    }
}
