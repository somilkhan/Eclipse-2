package io.github.somilkhan.instaeclipse2.mods.piko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LikeMetadataTrackerTest {

    enum Anim { ARES_LIKE_ACTIVATION, RINGS_LIKE_NINA, RINGS_LIKE_ZARNA }

    /** Shaped like XDTUserActivationMetadataImpl(animation, Boolean enabled, ...). */
    @SuppressWarnings("unused")
    static class Meta {
        static Anim DEFAULT = Anim.ARES_LIKE_ACTIVATION;
        final Anim A00;
        final Boolean A01;
        final Boolean A02;
        final String A03;

        Meta(Anim a, Boolean enabled, Boolean other, String s) {
            A00 = a;
            A01 = enabled;
            A02 = other;
            A03 = s;
        }
    }

    /** Unboxes the flag: can't be identified, so not tracked. */
    @SuppressWarnings("unused")
    static class Unboxed {
        final Anim a;
        final boolean enabled;

        Unboxed(Anim a, Boolean enabled) {
            this.a = a;
            this.enabled = enabled;
        }
    }

    private Object current;

    @Before
    public void setUp() {
        LikeMetadataTracker.clearForTest();
        LikeMetadataTracker.setPicker(() -> current);
    }

    @After
    public void tearDown() {
        LikeMetadataTracker.clearForTest();
    }

    /** What the constructor hook does: swap the args, construct, track. */
    private static Meta build(Anim orig, Boolean origEnabled, Anim pick) {
        Meta m = new Meta(pick, LikeMetadataTracker.MARK, Boolean.TRUE, "x");
        assertTrue(LikeMetadataTracker.track(m, pick, orig, origEnabled));
        return m;
    }

    @Test
    public void toggleOffRestoresInstagramsValues() {
        current = Anim.RINGS_LIKE_NINA;
        Meta m = build(Anim.ARES_LIKE_ACTIVATION, Boolean.FALSE, Anim.RINGS_LIKE_NINA);
        Meta n = build(null, null, Anim.RINGS_LIKE_NINA);
        current = null; // feature off
        LikeMetadataTracker.onFlagsChanged();
        assertEquals(Anim.ARES_LIKE_ACTIVATION, m.A00);
        assertEquals(Boolean.FALSE, m.A01);
        assertNull(n.A00);
        assertNull(n.A01);
        assertEquals(Boolean.TRUE, m.A02); // untouched
        assertEquals(0, LikeMetadataTracker.trackedCount());
    }

    @Test
    public void changingThePickRepointsBuiltModels() {
        current = Anim.RINGS_LIKE_NINA;
        Meta m = build(Anim.ARES_LIKE_ACTIVATION, Boolean.FALSE, Anim.RINGS_LIKE_NINA);
        current = Anim.RINGS_LIKE_ZARNA;
        LikeMetadataTracker.onFlagsChanged();
        assertEquals(Anim.RINGS_LIKE_ZARNA, m.A00);
        assertSame(LikeMetadataTracker.MARK, m.A01);
        assertEquals(1, LikeMetadataTracker.trackedCount());
        current = null;
        LikeMetadataTracker.onFlagsChanged();
        assertEquals(Anim.ARES_LIKE_ACTIVATION, m.A00);
        assertEquals(Boolean.FALSE, m.A01);
    }

    @Test
    public void unidentifiableModelIsNotTracked() {
        Unboxed u = new Unboxed(Anim.RINGS_LIKE_NINA, LikeMetadataTracker.MARK);
        assertFalse(LikeMetadataTracker.track(u, Anim.RINGS_LIKE_NINA, null, null));
        assertEquals(0, LikeMetadataTracker.trackedCount());
    }

    @Test
    public void markIsNotTheCachedTrue() {
        assertTrue(LikeMetadataTracker.MARK);
        assertFalse(LikeMetadataTracker.MARK == Boolean.TRUE);
    }
}
