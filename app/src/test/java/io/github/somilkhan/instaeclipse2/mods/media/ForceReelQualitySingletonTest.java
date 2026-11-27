package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ForceReelQualitySingletonTest {

    /** Stand-in for Guava's ImmutableList: a List type with a static copyOf(Collection). */
    public static class FakeImmutableList extends ArrayList<Object> {
        public static FakeImmutableList copyOf(Collection<?> c) {
            FakeImmutableList l = new FakeImmutableList();
            l.addAll(c);
            return l;
        }
    }

    @Test public void plainListReturnType() {
        Object r = ForceReelQualityHook.singleton("x", List.class);
        assertTrue(r instanceof List);
        assertEquals(1, ((List<?>) r).size());
    }

    @Test public void usesCopyOfForSpecificListTypes() {
        Object r = ForceReelQualityHook.singleton("x", FakeImmutableList.class);
        assertTrue(r instanceof FakeImmutableList);
        assertEquals("x", ((List<?>) r).get(0));
    }

    @Test public void givesUpWhenTheTypeCannotBeBuilt() {
        assertNull(ForceReelQualityHook.singleton("x", ArrayList.class));
    }
}
