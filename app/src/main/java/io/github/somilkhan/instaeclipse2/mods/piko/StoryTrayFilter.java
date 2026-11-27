package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Decision logic of the story tray filter (pure, unit tested). Mirrors Piko's FilterStory. */
public final class StoryTrayFilter {

    /** Piko's reel type values (piko_array_reel_type_val), in its order. */
    public static final String[] REEL_TYPES = {
            "ads_reel", "ar_effect_preview", "archive_day_media_reel", "archive_day_reel",
            "archive_friend_reel", "archive_map_reel", "archive_scheduled_for_deletion_reel",
            "archive_shop_reel", "axe_reel", "birthday_highlights", "bloks", "broadcast_reel",
            "daily_snapshot_reel", "election_reel", "empty_story_state_reel", "end_of_year",
            "expiring_story_reel", "fan_club_story", "follow_versaries", "friend_map_reel",
            "group_mention", "group_reel", "hallpass_reel", "highlight_reel", "highlight_rewind_reel",
            "inactive_hallpass_reel", "mas_reel", "media_reshare", "memory_reel", "my_week_reel",
            "netego_reel", "nux_reel", "private_story", "prompt_sticker_reel",
            "reconsideration_injected_story", "shopping_auto_highlight_reel", "shopping_pdp_reel",
            "shopping_smart_reel", "stars_welcome_reel", "story_stationary_timeline_reel",
            "subscription_shoutout_mention", "suggested_creator_reel", "suggested_user",
            "suggested_user_reel", "superlative_reel", "third_party_app_story", "typed_container",
            "user_reel",
    };

    public static final String VERIFIED = "verified";
    public static final String UNVERIFIED = "unverified";

    /** The account filter's choices (Piko's "users" filter is by verified state). */
    public static final String[] USER_CHOICES = {VERIFIED, UNVERIFIED};

    private StoryTrayFilter() {}

    public static Set<String> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public static String toCsv(Iterable<String> values) {
        StringBuilder b = new StringBuilder();
        for (String v : values) {
            if (v == null || v.isEmpty()) continue;
            if (b.length() > 0) b.append(',');
            b.append(v);
        }
        return b.toString();
    }

    /**
     * True when the tray item should be dropped. Unknown values never hide anything: a null reel
     * type matches no type, and a null verified state matches neither user filter.
     */
    public static boolean hide(String reelType, Boolean verified, Set<String> types, Set<String> users) {
        if (reelType != null && types.contains(reelType)) return true;
        if (verified != null) {
            if (verified && users.contains(VERIFIED)) return true;
            if (!verified && users.contains(UNVERIFIED)) return true;
        }
        return false;
    }

    /** Nothing selected: the hook can skip the list entirely. */
    public static boolean isNoop(Set<String> types, Set<String> users) {
        return types.isEmpty() && !users.contains(VERIFIED) && !users.contains(UNVERIFIED);
    }
}
