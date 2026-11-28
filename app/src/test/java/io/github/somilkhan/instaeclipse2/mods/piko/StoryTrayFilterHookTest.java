package io.github.somilkhan.instaeclipse2.mods.piko;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StoryTrayFilterHookTest {

    enum ReelType {
        USER("user_reel"), ADS("ads_reel");
        private final String server;
        ReelType(String s) { server = s; }
        @Override public String toString() { return server; }
    }

    /** Stands in for an Instagram enum whose toString() runs app code that fails. */
    enum Broken {
        A, ADS_REEL;
        @Override public String toString() {
            if (this == A) throw new IllegalStateException("Pando not ready");
            return "ads_reel";
        }
    }

    enum Unrelated { X, Y }

    @Test
    public void findsServerValue() {
        assertTrue(StoryTrayFilterHook.hasConstant(ReelType.class, "ads_reel"));
        assertFalse(StoryTrayFilterHook.hasConstant(Unrelated.class, "ads_reel"));
    }

    @Test
    public void throwingToStringOnlySkipsThatConstant() {
        assertTrue(StoryTrayFilterHook.hasConstant(Broken.class, "ads_reel"));
    }

    @Test
    public void nonEnumIsNotAMatch() {
        assertFalse(StoryTrayFilterHook.hasConstant(String.class, "ads_reel"));
    }
}
