package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Like animation choices (pure, unit tested). */
public final class LikeAnimations {

    public static final String RANDOM = "RANDOM";
    private static final String RINGS = "RINGS_LIKE_";

    /** 458.0.0.0.57 render enum names (X.052j), for the settings picker; resolved by name at runtime. */
    public static final String[] KNOWN = {
            "ANYWAY_LIKE_ACTIVATION", "RINGS_LIKE_ADRIAN", "RINGS_LIKE_AKI_KOICHI", "RINGS_LIKE_ARI",
            "RINGS_LIKE_BRIAN", "RINGS_LIKE_BRICKLEY", "RINGS_LIKE_COLE", "RINGS_LIKE_DJAG",
            "RINGS_LIKE_DOLLY", "RINGS_LIKE_ELYSE", "RINGS_LIKE_FILM", "RINGS_LIKE_FUTURA",
            "RINGS_LIKE_GABRIEL", "RINGS_LIKE_GOLLORIA", "RINGS_LIKE_HOMESTEAD", "RINGS_LIKE_KIDS",
            "RINGS_LIKE_LAUFEY", "RINGS_LIKE_LINDA", "RINGS_LIKE_MIMLES", "RINGS_LIKE_NIGEL",
            "RINGS_LIKE_NINA", "RINGS_LIKE_OLIVIA", "RINGS_LIKE_SEB", "RINGS_LIKE_TWINS",
            "RINGS_LIKE_TYSHAWN", "RINGS_LIKE_ZARNA",
    };

    private LikeAnimations() {}

    /** Real animations only: the enum's UNRECOGNIZED fallback (and anything unknown-shaped) is out. */
    public static boolean isSelectable(String name) {
        return name != null && (name.startsWith(RINGS) || name.endsWith("_LIKE_ACTIVATION"));
    }

    public static List<String> selectable(Iterable<String> names) {
        List<String> out = new ArrayList<>();
        for (String n : names) if (isSelectable(n)) out.add(n);
        return out;
    }

    /** "RINGS_LIKE_AKI_KOICHI" → "Aki Koichi", "ANYWAY_LIKE_ACTIVATION" → "Anyway". */
    public static String label(String name) {
        if (name == null) return "";
        if (RANDOM.equals(name)) return "Random";
        String core = name.startsWith(RINGS) ? name.substring(RINGS.length())
                : name.endsWith("_LIKE_ACTIVATION") ? name.substring(0, name.length() - "_LIKE_ACTIVATION".length())
                : name;
        StringBuilder b = new StringBuilder();
        for (String w : core.toLowerCase(Locale.ROOT).split("_")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    /**
     * Which constant to use: the saved name when this build has it, a random one for RANDOM, else
     * null (leave Instagram alone).
     */
    public static String pick(String pref, List<String> available, Random random) {
        List<String> ok = selectable(available);
        if (ok.isEmpty() || pref == null || pref.isEmpty()) return null;
        if (RANDOM.equals(pref)) return ok.get(random.nextInt(ok.size()));
        return ok.contains(pref) ? pref : null;
    }
}
