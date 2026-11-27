package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.BuildConfig;

public class ReleaseVersionTest {

    @Test
    public void versionIs090AboveOriginalBeta() {
        assertEquals("0.9.0", BuildConfig.VERSION_NAME);
        assertEquals(23, BuildConfig.VERSION_CODE);
        assertTrue("must update over the original 0.8.3 beta (code 21)", BuildConfig.VERSION_CODE > 21);
    }

    @Test
    public void targetsInstagram457() {
        assertEquals("457.0.0.55.73", ExportBrand.TESTED_INSTAGRAM);
        assertEquals("https://www.apkmirror.com/apk/instagram/instagram-instagram/instagram-457-0-0-55-73-release/",
                ExportBrand.TESTED_INSTAGRAM_APK);
    }
}
