package ps.reso.instaeclipse.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ps.reso.instaeclipse.BuildConfig;

public class ReleaseVersionTest {

    @Test
    public void versionIs084AboveOriginalBeta() {
        assertEquals("0.8.4", BuildConfig.VERSION_NAME);
        assertEquals(22, BuildConfig.VERSION_CODE);
        assertTrue("must update over the original 0.8.3 beta (code 21)", BuildConfig.VERSION_CODE > 21);
    }

    @Test
    public void targetsInstagram457() {
        assertEquals("457.0.0.55.73", ExportBrand.TESTED_INSTAGRAM);
        assertEquals("https://www.apkmirror.com/apk/instagram/instagram-instagram/instagram-457-0-0-55-73-release/",
                ExportBrand.TESTED_INSTAGRAM_APK);
    }
}
