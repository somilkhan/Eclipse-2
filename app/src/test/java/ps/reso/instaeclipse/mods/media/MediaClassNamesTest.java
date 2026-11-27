package ps.reso.instaeclipse.mods.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class MediaClassNamesTest {

    interface NewIntf {}
    interface OldIntf {}

    private static ClassLoader loader(String newName, Class<?> newCls, String oldName, Class<?> oldCls) {
        return new ClassLoader(MediaClassNamesTest.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(newName) && newCls != null) return newCls;
                if (name.equals(oldName) && oldCls != null) return oldCls;
                if (name.startsWith("com.instagram.")) throw new ClassNotFoundException(name);
                return super.loadClass(name);
            }
        };
    }

    @Test
    public void newestPackageWinsOn457() {
        String[] n = MediaClassNames.VIDEO_VERSION_INTF;
        assertEquals("com.instagram.api.schemas.VideoVersionIntf", n[0]);
        assertSame(NewIntf.class, MediaClassNames.loadFirst(loader(n[0], NewIntf.class, n[1], OldIntf.class), n));
    }

    @Test
    public void fallsBackToOldPackage() {
        String[] n = MediaClassNames.VIDEO_VERSION_INTF;
        assertSame(OldIntf.class, MediaClassNames.loadFirst(loader(n[0], null, n[1], OldIntf.class), n));
    }

    @Test
    public void nullWhenNoneExist() {
        String[] n = MediaClassNames.VIDEO_VERSION;
        assertNull(MediaClassNames.loadFirst(loader(n[0], null, n[1], null), n));
    }
}
