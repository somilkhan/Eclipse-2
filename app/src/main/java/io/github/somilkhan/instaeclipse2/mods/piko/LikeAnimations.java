package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Like animation choices (pure, unit tested). */
public final class LikeAnimations {

    public static final String RANDOM = "RANDOM";
    private static final String RINGS = "RINGS_LIKE_";
    /** Instagram's plain heart in the metadata enum: not a custom animation. */
    public static final String DEFAULT_HEART = "ARES_LIKE_ACTIVATION";

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
        return name != null && !DEFAULT_HEART.equals(name)
                && (name.startsWith(RINGS) || name.endsWith("_LIKE_ACTIVATION"));
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

    /**
     * Names the metadata and render enums spell differently, as groups of equivalent names. Only
     * listed spellings are treated as the same animation: a near-miss (BRIAN / ADRIAN) is a
     * different animation, not a typo.
     */
    static final String[][] ALIASES = {
            {"RINGS_LIKE_AKI_KOICHI", "RINGS_LIKE_AKI_KOCHI"},
    };

    /**
     * The constant in {@code names} for a picked name: the exact name, else a listed alias
     * ({@link #ALIASES}), else null (leave Instagram alone). Only selectable names are returned.
     */
    public static String metadataName(String pick, Iterable<String> names) {
        if (pick == null) return null;
        java.util.Set<String> wanted = new java.util.LinkedHashSet<>();
        wanted.add(pick);
        for (String[] group : ALIASES) {
            if (java.util.Arrays.asList(group).contains(pick)) wanted.addAll(java.util.Arrays.asList(group));
        }
        String alias = null;
        for (String n : names) {
            if (!isSelectable(n) || !wanted.contains(n)) continue;
            if (n.equals(pick)) return n;
            if (alias == null) alias = n;
        }
        return alias;
    }

    /**
     * Picked name → enum constant, resolved once per name (bind paths call this per view, so
     * the name matching must not run each time). Misses are cached too.
     */
    public static final class Resolver {
        private static final Object NONE = new Object();
        private final java.util.Map<String, Object> byName;
        private final List<String> names;
        private final java.util.concurrent.ConcurrentHashMap<String, Object> cache =
                new java.util.concurrent.ConcurrentHashMap<>();

        public Resolver(java.util.Map<String, Object> byName) {
            this.byName = byName;
            this.names = new ArrayList<>(byName.keySet());
        }

        public List<String> names() {
            return names;
        }

        /** Constant for {@code pick}, or null. */
        public Object get(String pick) {
            if (pick == null) return null;
            Object v = cache.get(pick);
            if (v == null) {
                String n = metadataName(pick, names);
                Object c = n != null ? byName.get(n) : null;
                v = c != null ? c : NONE;
                cache.put(pick, v);
            }
            return v == NONE ? null : v;
        }
    }
}
