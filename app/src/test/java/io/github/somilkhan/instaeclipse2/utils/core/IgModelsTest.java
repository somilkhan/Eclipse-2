package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class IgModelsTest {

    /** Shaped like a 458 Pando model: one generic accessor keyed by the field name's hashCode. */
    public static class FakeModel {
        public final Object BvS(int key) {
            if (key == "has_liked".hashCode()) return Boolean.TRUE;
            if (key == "username".hashCode()) return "zen";
            if (key == "video_versions".hashCode()) return Arrays.asList("a", "b");
            return null;
        }

        public String getTypeName() { return "XDTMediaDict"; }
    }

    public static class FakeChild extends FakeModel {}

    /** Two candidates: ambiguous, so no accessor. */
    public static class Ambiguous {
        public Object a(int k) { return null; }
        public Object b(int k) { return null; }
    }

    public static class NoAccessor {
        public String get(int k) { return null; }
        public static Object s(int k) { return null; }
    }

    @Before public void reset() { IgModels.resetForTest(); }

    @Test public void readsFieldsByHash() {
        FakeModel m = new FakeModel();
        assertEquals(Boolean.TRUE, IgModels.readBoolean(m, "has_liked"));
        assertEquals("zen", IgModels.readString(m, "username"));
        List<?> l = IgModels.readList(m, "video_versions");
        assertNotNull(l);
        assertEquals(2, l.size());
        assertNull(IgModels.read(m, "caption"));
        assertNull(IgModels.readString(m, "has_liked"));
    }

    @Test public void accessorFoundOnSuperclass() {
        assertTrue(IgModels.readable(new FakeChild()));
        assertEquals("zen", IgModels.readString(new FakeChild(), "username"));
    }

    @Test public void noGuessingWhenAmbiguousOrMissing() {
        assertNull(IgModels.accessor(Ambiguous.class));
        assertNull(IgModels.accessor(NoAccessor.class));
        assertFalse(IgModels.readable(new NoAccessor()));
        assertNull(IgModels.read(new NoAccessor(), "username"));
        assertNull(IgModels.read(null, "username"));
    }

    @Test public void legacyUserNameByDefault() {
        assertEquals(IgModels.LEGACY_USER, IgModels.userClassName());
        assertTrue(IgModels.isUserClass(IgModels.LEGACY_USER));
        assertFalse(IgModels.isUser(new FakeModel()));
        assertFalse(IgModels.isUserClass(null));
    }
}
