package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Field sets taken from Instagram 458.0.0.0.57 (config X.083g): base check X.0CKn.A00 reads
 * {A07,A08}; the inlined gate in lambda X.0JBF.A09 reads {A0B,A0C,A0F,A0G}; the touch listener
 * X.0Ji2.onTouch reads those plus {A09,A0A,A0D,A0E,A0K}.
 */
public class ReelScrubberFlagsTest {

    private static Set<String> s(String... v) {
        return new TreeSet<>(Arrays.asList(v));
    }

    @Test
    public void picksFlagsEveryGateSiteReads() {
        Set<String> base = s("A07", "A08");
        List<Set<String>> callers = Arrays.asList(
                s("A0B", "A0C", "A0F", "A0G"),
                s("A09", "A0A", "A0B", "A0C", "A0D", "A0E", "A0F", "A0G", "A0K"));
        assertEquals(s("A0B", "A0C", "A0F", "A0G"), PlaybackSpeedHook.selectScrubberFlags(base, callers));
    }

    @Test
    public void callersReadingOnlyBaseFieldsAreNotGateSites() {
        Set<String> base = s("A07", "A08");
        List<Set<String>> callers = Arrays.asList(s("A07"), s(), s("A0B", "A0C", "A0F", "A0G"));
        assertEquals(s("A0B", "A0C", "A0F", "A0G"), PlaybackSpeedHook.selectScrubberFlags(base, callers));
    }

    @Test
    public void disjointCallersGiveNothing() {
        Set<String> base = s("A07");
        List<Set<String>> callers = Arrays.asList(s("A0B"), s("A0C"));
        assertTrue(PlaybackSpeedHook.selectScrubberFlags(base, callers).isEmpty());
    }

    @Test
    public void noCallersOrTooManyFlagsGiveNothing() {
        assertTrue(PlaybackSpeedHook.selectScrubberFlags(s("A07"), Collections.emptyList()).isEmpty());
        List<Set<String>> wide = Collections.singletonList(s("A0A", "A0B", "A0C", "A0D", "A0E", "A0F", "A0G"));
        assertTrue(PlaybackSpeedHook.selectScrubberFlags(s(), wide).isEmpty());
    }
}
