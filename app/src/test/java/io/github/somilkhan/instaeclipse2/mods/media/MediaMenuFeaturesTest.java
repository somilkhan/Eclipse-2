package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

public class MediaMenuFeaturesTest {

    private boolean[] saved;

    @Before
    public void setUp() {
        saved = new boolean[] {FeatureFlags.enablePostDownload, FeatureFlags.enableReelDownload,
                FeatureFlags.copyMediaLink, FeatureFlags.offerCopyImage, FeatureFlags.offerMediaInfo,
                FeatureFlags.offerPlaybackSpeed, FeatureFlags.externalDownloader};
        set(false, false, false, false, false, false, false);
    }

    @After
    public void tearDown() {
        set(saved[0], saved[1], saved[2], saved[3], saved[4], saved[5], saved[6]);
    }

    private static void set(boolean post, boolean reel, boolean link, boolean image, boolean info,
                            boolean speed, boolean external) {
        FeatureFlags.enablePostDownload = post;
        FeatureFlags.enableReelDownload = reel;
        FeatureFlags.copyMediaLink = link;
        FeatureFlags.offerCopyImage = image;
        FeatureFlags.offerMediaInfo = info;
        FeatureFlags.offerPlaybackSpeed = speed;
        FeatureFlags.externalDownloader = external;
    }

    @Test
    public void nothingEnabled() {
        assertFalse(MediaMenuFeatures.anyPostMenuFeature());
        assertFalse(MediaMenuFeatures.anyReelMenuFeature());
        assertFalse(MediaMenuFeatures.anyMenuFeature());
    }

    @Test
    public void externalDownloaderAloneOpensTheClickGate() {
        FeatureFlags.externalDownloader = true;
        assertTrue(MediaMenuFeatures.anyPostMenuFeature());
        assertTrue(MediaMenuFeatures.anyReelMenuFeature());
        assertTrue(MediaMenuFeatures.anyMenuFeature());
    }

    @Test
    public void eachFeatureAloneOpensTheClickGate() {
        for (int i = 0; i < 7; i++) {
            boolean[] b = new boolean[7];
            b[i] = true;
            set(b[0], b[1], b[2], b[3], b[4], b[5], b[6]);
            assertTrue("flag " + i, MediaMenuFeatures.anyMenuFeature());
        }
    }

    @Test
    public void postOnlyFlagsDoNotOpenReelMenu() {
        FeatureFlags.enablePostDownload = true;
        FeatureFlags.offerCopyImage = true;
        assertFalse(MediaMenuFeatures.anyReelMenuFeature());
        FeatureFlags.enableReelDownload = true;
        assertTrue(MediaMenuFeatures.anyReelMenuFeature());
    }
}
