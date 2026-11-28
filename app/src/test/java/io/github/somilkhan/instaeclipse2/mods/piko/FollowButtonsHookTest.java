package io.github.somilkhan.instaeclipse2.mods.piko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Shape of Instagram 457.0.0.55.73: the component base (X.0HCo) declares the final bind method
 * and a virtual name(); only the follow component (X.058K, two levels down) overrides name().
 */
public class FollowButtonsHookTest {

    abstract static class Base {
        public final void bind(Object o) { }
        public String name() { return "Base"; }
    }

    abstract static class Middle extends Base { }

    static final class FollowComponent extends Middle {
        @Override public String name() { return "StoryFollowButtonViewComponent"; }
    }

    static final class OtherComponent extends Middle {
        @Override public String name() { return "Other"; }
    }

    static final class Unrelated {
        public String name() { return "StoryFollowButtonViewComponent"; }
    }

    @Test
    public void overrideInSubclassResolvesToTheBaseDeclaration() throws Exception {
        Method found = FollowComponent.class.getDeclaredMethod("name");
        Method base = FollowButtonsHook.baseNameGetter(Base.class, found);
        assertEquals(Base.class.getDeclaredMethod("name"), base);
        // Virtual dispatch: the same Method tells components apart.
        assertEquals("StoryFollowButtonViewComponent", base.invoke(new FollowComponent()));
        assertEquals("Other", base.invoke(new OtherComponent()));
    }

    @Test
    public void nameDeclaredOnTheBaseIsUsedAsIs() throws Exception {
        Method m = Base.class.getDeclaredMethod("name");
        assertEquals(m, FollowButtonsHook.baseNameGetter(Base.class, m));
    }

    @Test
    public void unrelatedClassesAreRejected() throws Exception {
        assertNull(FollowButtonsHook.baseNameGetter(Base.class, Unrelated.class.getDeclaredMethod("name")));
        assertNull(FollowButtonsHook.baseNameGetter(null, Unrelated.class.getDeclaredMethod("name")));
        assertNull(FollowButtonsHook.baseNameGetter(Base.class, null));
    }
}
