package io.github.somilkhan.instaeclipse2.mods.piko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Pure helpers behind the second batch of Piko ports. */
public class PikoHelpersTest {

    // ── CallButtons ────────────────────────────────────────────────────────────

    @Test public void callKindsAreCalls() {
        for (String k : new String[]{"AUDIO_CALL", "VIDEO_CALL", "COMBINED_CALL_V1", "COMBINED_CALL_V4", "COLLAPSED_CALL",
                "ONGOING_AUDIO_CALL", "ONGOING_VIDEO_CALL", "AI_AUDIO_CALL", "WHATSAPP_VIDEO_CALL"})
            assertTrue(k, CallButtons.isCall(k));
    }

    @Test public void otherHeaderButtonsStay() {
        for (String k : new String[]{"INFO", "BLEND", "SPEAKER", "MORE", "CALLS_TAB", "", null})
            assertFalse(String.valueOf(k), CallButtons.isCall(k));
    }

    // ── StoryTrayFilter ────────────────────────────────────────────────────────

    @Test public void csvRoundTripTrimsAndDedupes() {
        Set<String> s = StoryTrayFilter.parseCsv(" ads_reel, ,mas_reel,ads_reel ");
        assertEquals(Arrays.asList("ads_reel", "mas_reel"), new java.util.ArrayList<>(s));
        assertEquals("ads_reel,mas_reel", StoryTrayFilter.toCsv(s));
        assertTrue(StoryTrayFilter.parseCsv(null).isEmpty());
        assertTrue(StoryTrayFilter.parseCsv("  ").isEmpty());
    }

    @Test public void hidesByTypeAndVerifiedState() {
        Set<String> types = StoryTrayFilter.parseCsv("ads_reel");
        Set<String> users = StoryTrayFilter.parseCsv(StoryTrayFilter.VERIFIED);
        assertTrue(StoryTrayFilter.hide("ads_reel", false, types, users));
        assertTrue(StoryTrayFilter.hide("user_reel", true, types, users));
        assertFalse(StoryTrayFilter.hide("user_reel", false, types, users));
        // Unknown values never hide anything.
        assertFalse(StoryTrayFilter.hide(null, null, types, users));
        assertTrue(StoryTrayFilter.isNoop(Collections.emptySet(), Collections.emptySet()));
        assertFalse(StoryTrayFilter.isNoop(types, Collections.emptySet()));
    }

    @Test public void userChoicesAreTheVerifiedStates() {
        assertEquals(Arrays.asList(StoryTrayFilter.VERIFIED, StoryTrayFilter.UNVERIFIED), Arrays.asList(StoryTrayFilter.USER_CHOICES));
        assertTrue(Arrays.asList(StoryTrayFilter.REEL_TYPES).contains("user_reel"));
    }

    // ── LikeAnimations ─────────────────────────────────────────────────────────

    @Test public void labelsAreReadable() {
        assertEquals("Aki Koichi", LikeAnimations.label("RINGS_LIKE_AKI_KOICHI"));
        assertEquals("Anyway", LikeAnimations.label("ANYWAY_LIKE_ACTIVATION"));
        assertEquals("Random", LikeAnimations.label(LikeAnimations.RANDOM));
    }

    @Test public void unrecognizedIsNeverPicked() {
        List<String> avail = Arrays.asList("UNRECOGNIZED", "RINGS_LIKE_NINA", "ANYWAY_LIKE_ACTIVATION");
        assertEquals(Arrays.asList("RINGS_LIKE_NINA", "ANYWAY_LIKE_ACTIVATION"), LikeAnimations.selectable(avail));
        assertNull(LikeAnimations.pick("UNRECOGNIZED", avail, new Random(1)));
        assertEquals("RINGS_LIKE_NINA", LikeAnimations.pick("RINGS_LIKE_NINA", avail, new Random(1)));
        assertNull(LikeAnimations.pick("RINGS_LIKE_GONE", avail, new Random(1)));
        for (int i = 0; i < 50; i++) {
            String r = LikeAnimations.pick(LikeAnimations.RANDOM, avail, new Random(i));
            assertTrue(r, LikeAnimations.isSelectable(r));
        }
        assertNull(LikeAnimations.pick(LikeAnimations.RANDOM, Collections.singletonList("UNRECOGNIZED"), new Random()));
    }

    @Test public void defaultHeartIsNotACustomAnimation() {
        assertTrue(!LikeAnimations.isSelectable(LikeAnimations.DEFAULT_HEART));
        assertNull(LikeAnimations.pick(LikeAnimations.RANDOM,
                Collections.singletonList(LikeAnimations.DEFAULT_HEART), new Random(1)));
    }

    @Test public void metadataNameMatchesAcrossEnums() {
        java.util.List<String> meta = Arrays.asList("ARES_LIKE_ACTIVATION", "RINGS_LIKE_AKI_KOCHI",
                "RINGS_LIKE_NINA", "RINGS_LIKE_NIGEL", "UNRECOGNIZED");
        assertEquals("RINGS_LIKE_NINA", LikeAnimations.metadataName("RINGS_LIKE_NINA", meta));
        assertEquals("RINGS_LIKE_AKI_KOCHI", LikeAnimations.metadataName("RINGS_LIKE_AKI_KOICHI", meta));
        assertNull(LikeAnimations.metadataName("RINGS_LIKE_ZARNA", meta));
        assertNull(LikeAnimations.metadataName("ARES_LIKE_ACTIVATION", meta));
        assertNull(LikeAnimations.metadataName(null, meta));
        // Near-misses are different animations, not spellings: no fuzzy matching.
        assertNull(LikeAnimations.metadataName("RINGS_LIKE_NINO", meta));
        // Aliases work both ways, and an exact name wins over its alias.
        List<String> render = Arrays.asList("RINGS_LIKE_AKI_KOICHI", "UNRECOGNIZED");
        assertEquals("RINGS_LIKE_AKI_KOICHI", LikeAnimations.metadataName("RINGS_LIKE_AKI_KOCHI", render));
        assertEquals("RINGS_LIKE_AKI_KOICHI", LikeAnimations.metadataName("RINGS_LIKE_AKI_KOICHI",
                Arrays.asList("RINGS_LIKE_AKI_KOCHI", "RINGS_LIKE_AKI_KOICHI")));
    }

    @Test public void similarNamesAreNotSwapped() {
        List<String> onlyAdrian = Arrays.asList("RINGS_LIKE_ADRIAN", "RINGS_LIKE_NINA", "UNRECOGNIZED");
        assertNull(LikeAnimations.metadataName("RINGS_LIKE_BRIAN", onlyAdrian));
        assertNull(LikeAnimations.metadataName("RINGS_LIKE_LINDA", onlyAdrian));
    }

    enum FakeAnim { UNRECOGNIZED, ARES_LIKE_ACTIVATION, RINGS_LIKE_AKI_KOCHI, RINGS_LIKE_NINA }

    @Test public void resolverMapsAndCachesPicks() {
        java.util.Map<String, Object> byName = new java.util.HashMap<>();
        for (FakeAnim a : FakeAnim.values()) byName.put(a.name(), a);
        LikeAnimations.Resolver r = new LikeAnimations.Resolver(byName);
        assertEquals(FakeAnim.RINGS_LIKE_NINA, r.get("RINGS_LIKE_NINA"));
        assertEquals(FakeAnim.RINGS_LIKE_AKI_KOCHI, r.get("RINGS_LIKE_AKI_KOICHI"));
        assertNull(r.get("RINGS_LIKE_NINO"));
        assertNull(r.get("ARES_LIKE_ACTIVATION"));
        assertNull(r.get("UNRECOGNIZED"));
        assertNull(r.get(null));
        byName.clear(); // cached: no further lookups in the map
        assertEquals(FakeAnim.RINGS_LIKE_NINA, r.get("RINGS_LIKE_NINA"));
        assertNull(r.get("RINGS_LIKE_NINO"));
    }

    @Test public void knownNamesAreSelectable() {
        for (String k : LikeAnimations.KNOWN) assertTrue(k, LikeAnimations.isSelectable(k));
    }

    // ── StoryRingSize ──────────────────────────────────────────────────────────

    @Test public void ringSizeClampsAndOnlyReplacesInstagramBounds() {
        assertEquals(40, StoryRingSize.clamp(0));
        assertEquals(160, StoryRingSize.clamp(999));
        assertEquals(120, StoryRingSize.clamp(120));
        assertEquals(120f, StoryRingSize.replace(66f, 120), 0f);
        assertEquals(120f, StoryRingSize.replace(100f, 120), 0f);
        assertEquals(12f, StoryRingSize.replace(12f, 120), 0f);
    }

    // ── ReelResumeStore ────────────────────────────────────────────────────────

    @Test public void resumeStoreSkipsShortPositionsAndTakesOnce() {
        ReelResumeStore s = new ReelResumeStore(2);
        s.save("a", 500);
        assertEquals(-1, s.take("a"));
        s.save("a", 4000);
        assertEquals(4000, s.take("a"));
        assertEquals(-1, s.take("a"));
        s.save("b", 2000);
        s.save("b", 10); // a near-start pause clears the old one
        assertEquals(-1, s.take("b"));
    }

    @Test public void resumeStoreIsBounded() {
        ReelResumeStore s = new ReelResumeStore(2);
        s.save("a", 2000);
        s.save("b", 2000);
        s.save("c", 2000);
        assertEquals(2, s.size());
        assertEquals(-1, s.take("a"));
        assertEquals(2000, s.take("c"));
    }

    // ── PostLinks ──────────────────────────────────────────────────────────────

    @Test public void shortcodeIsBase64UrlOfThePk() {
        assertEquals("B", PostLinks.shortcode("1"));
        assertEquals("_", PostLinks.shortcode("63"));
        assertEquals("BA", PostLinks.shortcode("64"));
        assertEquals("BA", PostLinks.shortcode("64_12345")); // "pk_ownerId" ids
        assertEquals("", PostLinks.shortcode("0"));
        assertEquals("", PostLinks.shortcode("abc"));
        assertEquals("", PostLinks.shortcode(null));
    }

    @Test public void shortcodeRoundTrips() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        String pk = "3016584049378017367";
        java.math.BigInteger n = java.math.BigInteger.ZERO;
        for (char c : PostLinks.shortcode(pk).toCharArray())
            n = n.multiply(java.math.BigInteger.valueOf(64)).add(java.math.BigInteger.valueOf(alphabet.indexOf(c)));
        assertEquals(pk, n.toString());
    }

    @Test public void permalinkPicksReelOrPost() {
        assertEquals("https://www.instagram.com/reel/ABC/", PostLinks.permalink("ABC", "clips", null));
        assertEquals("https://www.instagram.com/p/ABC/", PostLinks.permalink("ABC", "feed", null));
        assertEquals("https://www.instagram.com/p/BA/", PostLinks.permalink(null, null, "64_1"));
        assertEquals("https://www.instagram.com/p/BA/", PostLinks.permalink("  ", null, "64"));
        assertNull(PostLinks.permalink(null, "clips", null));
    }

    // ── PendingReceipts ────────────────────────────────────────────────────────

    @Test public void pendingKeepsNewestPerThreadAndReleasesOnce() {
        PendingReceipts<String> p = new PendingReceipts<>(8);
        p.hold("t1", "m1");
        p.hold("t1", "m2");
        p.hold("", "x");
        p.hold(null, "x");
        p.hold("t2", null);
        assertEquals(1, p.size());
        assertEquals("m2", p.release("t1"));
        assertNull(p.release("t1"));
        assertNull(p.release(null));
    }

    @Test public void pendingIsBounded() {
        PendingReceipts<Integer> p = new PendingReceipts<>(2);
        p.hold("a", 1);
        p.hold("b", 2);
        p.hold("c", 3);
        assertEquals(2, p.size());
        assertNull(p.release("a"));
        assertEquals(Integer.valueOf(3), p.release("c"));
    }

    // ── ThreadKeyIds ───────────────────────────────────────────────────────────

    @Test public void threadKeyIdsMatchInstagram458Format() {
        // DirectThreadKey.toString() on 458 has no closing quote after the v2 id.
        assertEquals(Arrays.asList("340282366841710300949128", "17843"),
                ThreadKeyIds.parse("DirectThreadKey{mThreadId='340282366841710300949128', mThreadV2Id='17843}"));
        assertEquals(Arrays.asList("1", "2"), ThreadKeyIds.parse("DirectThreadKey{mThreadId='1', mThreadV2Id='2'}"));
    }

    @Test public void threadKeyIdsSkipNullAndDuplicates() {
        assertEquals(Collections.singletonList("1"), ThreadKeyIds.parse("DirectThreadKey{mThreadId='1', mThreadV2Id='null}"));
        assertEquals(Collections.singletonList("1"), ThreadKeyIds.parse("DirectThreadKey{mThreadId='1', mThreadV2Id='1}"));
        assertTrue(ThreadKeyIds.parse("DirectThreadKey{mThreadId='null', mThreadV2Id='}").isEmpty());
        assertTrue(ThreadKeyIds.parse(null).isEmpty());
        assertTrue(ThreadKeyIds.parse("something else").isEmpty());
    }
}
