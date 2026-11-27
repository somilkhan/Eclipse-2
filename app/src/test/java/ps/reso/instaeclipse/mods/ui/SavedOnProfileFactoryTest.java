package ps.reso.instaeclipse.mods.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SavedOnProfileFactoryTest {
    static class Fragment {}
    static class SavedFragment extends Fragment {}
    static class Session {}

    @Test
    public void acceptsTwoFlagFactoryOfOlderBuilds() {
        assertTrue(SavedOnProfileHook.isSavedFactory(
                new Class<?>[]{Session.class, String.class, boolean.class, boolean.class}, SavedFragment.class, Fragment.class));
    }

    @Test
    public void acceptsThreeFlagFactoryOf457() {
        // X.0P5K.A00(UserSession, String, boolean, boolean, boolean) on 457
        assertTrue(SavedOnProfileHook.isSavedFactory(
                new Class<?>[]{Session.class, String.class, boolean.class, boolean.class, boolean.class},
                SavedFragment.class, Fragment.class));
    }

    @Test
    public void rejectsOtherShapes() {
        assertFalse(SavedOnProfileHook.isSavedFactory(
                new Class<?>[]{Session.class, String.class, boolean.class, int.class}, SavedFragment.class, Fragment.class));
        assertFalse(SavedOnProfileHook.isSavedFactory(
                new Class<?>[]{Session.class, String.class, boolean.class, boolean.class}, Object.class, Fragment.class));
        assertFalse(SavedOnProfileHook.isSavedFactory(
                new Class<?>[]{Session.class, String.class, boolean.class}, SavedFragment.class, Fragment.class));
    }

    @Test
    public void argsOpenPlainSavedScreen() {
        Object s = new Object();
        assertArrayEquals(new Object[]{s, null, false, false}, SavedOnProfileHook.savedFactoryArgs(s, 4));
        assertArrayEquals(new Object[]{s, null, false, false, false}, SavedOnProfileHook.savedFactoryArgs(s, 5));
    }
}
