package io.github.somilkhan.instaeclipse2.utils.download;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SafGrantsTest {
    private static final String A = "content://com.android.externalstorage.documents/tree/primary%3ADownload";
    private static final String B = "content://com.android.externalstorage.documents/tree/primary%3AMovies";

    @Test public void releasesTheReplacedFolder() { assertTrue(SafGrants.shouldRelease(A, B)); }
    @Test public void keepsAFolderAnotherSettingStillUses() { assertFalse(SafGrants.shouldRelease(A, B, A)); }
    @Test public void nothingToReleaseWhenUnchangedOrUnset() {
        assertFalse(SafGrants.shouldRelease(A, A));
        assertFalse(SafGrants.shouldRelease("", B));
        assertFalse(SafGrants.shouldRelease(null, B));
    }
}
