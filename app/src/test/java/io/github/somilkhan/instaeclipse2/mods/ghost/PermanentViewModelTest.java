package io.github.somilkhan.instaeclipse2.mods.ghost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PermanentViewModelTest {

    @SuppressWarnings("unused")
    static class Named {
        static int CONSTANT = 7; // static: must be ignored
        int width = 1080;
        int seen_count;
        String view_mode;
        String other = "x";
        int height = 1920; // declared after seen_count: the old code picked the last int

        Named(int seen, String mode) {
            seen_count = seen;
            view_mode = mode;
        }
    }

    @SuppressWarnings("unused")
    static class ObfuscatedSingle {
        static int A00 = 5;
        int A01;
        String A02;
        String A03;

        ObfuscatedSingle(int seen, String mode) {
            A01 = seen;
            A03 = mode;
        }
    }

    @SuppressWarnings("unused")
    static class ObfuscatedAmbiguous {
        int A00 = 1080;
        int A01;
        String A02;

        ObfuscatedAmbiguous(int seen, String mode) {
            A01 = seen;
            A02 = mode;
        }
    }

    static class Base {
        int seenCount;
    }

    @SuppressWarnings("unused")
    static class Sub extends Base {
        int A00 = 3;
        String mode = "replayable";
    }

    private static boolean convert(Object model) {
        PermanentViewModel.SeenCount sc = PermanentViewModel.resolveSeenCount(model.getClass());
        int seen = PermanentViewModel.readSeenCount(model, sc);
        String original = PermanentViewModel.originalMode(model);
        if (original == null || PermanentViewModel.consumed(original, seen)) return false;
        return PermanentViewModel.rewriteViewModes(model, "permanent") > 0;
    }

    @Test
    public void namedFieldWinsOverOtherInts() {
        PermanentViewModel.SeenCount sc = PermanentViewModel.resolveSeenCount(Named.class);
        assertEquals("seen_count", sc.field.getName());
        assertSame(sc, PermanentViewModel.resolveSeenCount(Named.class)); // cached
    }

    @Test
    public void unseenOnceIsConvertedDespiteLargeOtherInts() {
        Named m = new Named(0, "once");
        assertTrue(convert(m));
        assertEquals("permanent", m.view_mode);
        assertEquals("x", m.other);
    }

    @Test
    public void seenOnceIsLeftAlone() {
        Named m = new Named(1, "once");
        assertFalse(convert(m));
        assertEquals("once", m.view_mode);
    }

    @Test
    public void replayableAllowsTwoViews() {
        Named once = new Named(1, "replayable");
        assertTrue(convert(once));
        assertEquals("permanent", once.view_mode);
        Named twice = new Named(2, "allow_replay");
        assertFalse(convert(twice));
        assertEquals("allow_replay", twice.view_mode);
    }

    @Test
    public void singleInstanceIntIsUsedAndStaticIgnored() {
        PermanentViewModel.SeenCount sc = PermanentViewModel.resolveSeenCount(ObfuscatedSingle.class);
        assertEquals("A01", sc.field.getName());
        ObfuscatedSingle seen = new ObfuscatedSingle(1, "once");
        assertFalse(convert(seen));
        ObfuscatedSingle fresh = new ObfuscatedSingle(0, "once");
        assertTrue(convert(fresh));
        assertEquals("permanent", fresh.A03);
        assertNull(fresh.A02);
    }

    @Test
    public void ambiguousIntsSkipTheGate() {
        PermanentViewModel.SeenCount sc = PermanentViewModel.resolveSeenCount(ObfuscatedAmbiguous.class);
        assertNull(sc.field);
        assertTrue(sc.unresolvedReason.startsWith("ambiguous"));
        ObfuscatedAmbiguous m = new ObfuscatedAmbiguous(1, "once");
        assertEquals(-1, PermanentViewModel.readSeenCount(m, sc));
        assertTrue(convert(m));
        assertEquals("permanent", m.A02);
    }

    @Test
    public void namedFieldInSuperclassIsFound() {
        assertEquals("seenCount", PermanentViewModel.resolveSeenCount(Sub.class).field.getName());
        Sub s = new Sub();
        s.seenCount = 2;
        assertFalse(convert(s));
        assertEquals("replayable", s.mode);
    }

    @Test
    public void allMatchingStringFieldsAreRewritten() {
        ObfuscatedSingle m = new ObfuscatedSingle(0, "once");
        m.A02 = "replayable";
        assertEquals("once", PermanentViewModel.originalMode(m));
        assertTrue(convert(m));
        assertEquals("permanent", m.A02);
        assertEquals("permanent", m.A03);
    }

    @Test
    public void nonEphemeralIsUntouched() {
        Named m = new Named(0, "permanent");
        assertFalse(convert(m));
        assertNull(PermanentViewModel.originalMode(new Named(0, null)));
    }
}
