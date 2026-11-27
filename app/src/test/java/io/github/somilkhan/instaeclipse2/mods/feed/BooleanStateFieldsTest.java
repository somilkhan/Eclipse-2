package io.github.somilkhan.instaeclipse2.mods.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class BooleanStateFieldsTest {

    // Built from the hook's compile-time constants (inlined), so the test never loads the
    // Xposed-dependent hook class on the JVM.
    private static final java.util.List<String> LABELS = Arrays.asList(
            FeedUfiButtonsHook.SHARE, FeedUfiButtonsHook.SHARE_COUNT, FeedUfiButtonsHook.SAVE,
            FeedUfiButtonsHook.REPOST, FeedUfiButtonsHook.LIKE_COUNT);

    /**
     * Shaped like Instagram's obfuscated MediaUfiUiState (X.04d9 on 458): final booleans with
     * meaningless names, declared in a different order than printed, and one field
     * (A0Q, the repost flag) read last, as the compiler emits it on 458.
     */
    static final class FakeUfiState {
        final boolean A0Q; // isRepostButtonEnabled
        final boolean A0S; // isSaveEnabled
        final boolean A0V; // isShareEnabled
        final boolean A0c; // shouldShowLikeCountInUfi
        final boolean A0e; // shouldShowShareCountInUfi
        final boolean A0M; // isLiked (not a target)
        final Object media = "Media{id=1}";

        FakeUfiState(boolean all) {
            A0Q = all; A0S = all; A0V = all; A0c = all; A0e = all; A0M = !all;
        }

        @Override
        public String toString() {
            boolean repost = A0Q;
            return "MediaUfiUiState(mediaForActionHandler=" + media
                    + ", isLiked=" + A0M
                    + ", isSaved=" + false
                    + ", isShareEnabled=" + A0V
                    + ", isSaveEnabled=" + A0S
                    + ", shouldShowLikeCountInUfi=" + A0c
                    + ", shouldShowShareCountInUfi=" + A0e
                    + ", isRepostButtonEnabled=" + repost + ")";
        }
    }

    @Test
    public void resolvesEveryLabelToItsField() {
        FakeUfiState s = new FakeUfiState(true);
        Map<String, Field> m = BooleanStateFields.resolve(s, LABELS);
        assertEquals("A0V", m.get(FeedUfiButtonsHook.SHARE).getName());
        assertEquals("A0e", m.get(FeedUfiButtonsHook.SHARE_COUNT).getName());
        assertEquals("A0S", m.get(FeedUfiButtonsHook.SAVE).getName());
        assertEquals("A0Q", m.get(FeedUfiButtonsHook.REPOST).getName());
        assertEquals("A0c", m.get(FeedUfiButtonsHook.LIKE_COUNT).getName());
    }

    @Test
    public void restoresEveryValue() {
        FakeUfiState s = new FakeUfiState(true);
        String before = s.toString();
        BooleanStateFields.resolve(s, LABELS);
        assertEquals(before, s.toString());
    }

    @Test
    public void resolvedFieldClearsOnlyThatControl() throws Exception {
        FakeUfiState s = new FakeUfiState(true);
        Field share = BooleanStateFields.resolve(s, LABELS).get(FeedUfiButtonsHook.SHARE);
        share.setBoolean(s, false);
        assertEquals("false", BooleanStateFields.valueAfter(s.toString(), FeedUfiButtonsHook.SHARE));
        assertEquals("true", BooleanStateFields.valueAfter(s.toString(), FeedUfiButtonsHook.SAVE));
        assertFalse(s.A0V);
        assertTrue(s.A0S);
    }

    @Test
    public void missingLabelIsLeftOut() {
        Map<String, Field> m = BooleanStateFields.resolve(new FakeUfiState(true),
                Arrays.asList(FeedUfiButtonsHook.SHARE, ", isGoneInThisBuild="));
        assertEquals(1, m.size());
        assertNull(m.get(", isGoneInThisBuild="));
    }

    /** Two fields feeding one label (e.g. "a && b"): unsafe to pick one, so nothing is returned. */
    static final class Ambiguous {
        // Set in the constructor: "= true" would be a compile-time constant that toString inlines.
        final boolean a;
        final boolean b;
        Ambiguous() { a = true; b = true; }
        @Override public String toString() { return "S(, flag=" + (a && b) + ")"; }
    }

    @Test
    public void ambiguousLabelIsLeftOut() {
        assertTrue(BooleanStateFields.resolve(new Ambiguous(), Collections.singletonList(", flag=")).isEmpty());
    }

    @Test
    public void valueAfterParsesOnlyBooleans() {
        assertEquals("true", BooleanStateFields.valueAfter("X(, a=true, b=false)", ", a="));
        assertEquals("false", BooleanStateFields.valueAfter("X(, a=true, b=false)", ", b="));
        assertNull(BooleanStateFields.valueAfter("X(, a=12)", ", a="));
        assertNull(BooleanStateFields.valueAfter("X()", ", a="));
        assertNull(BooleanStateFields.valueAfter(null, ", a="));
    }

    @Test
    public void nullInputsAreSafe() {
        assertTrue(BooleanStateFields.resolve(null, LABELS).isEmpty());
        assertTrue(BooleanStateFields.resolve(new FakeUfiState(true), null).isEmpty());
    }
}
