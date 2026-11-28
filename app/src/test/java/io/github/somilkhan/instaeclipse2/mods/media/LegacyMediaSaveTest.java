package io.github.somilkhan.instaeclipse2.mods.media;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

/** MEDIA-01/02/04: legacy in-process saves reuse the verified fetch and atomic publisher. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class LegacyMediaSaveTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private String path, uri;
    private boolean nest;

    @Before public void remember() {
        path = FeatureFlags.downloaderCustomPath;
        uri = FeatureFlags.downloaderCustomUri;
        nest = FeatureFlags.downloaderUsernameFolder;
    }

    @After public void restore() {
        FeatureFlags.downloaderCustomPath = path;
        FeatureFlags.downloaderCustomUri = uri;
        FeatureFlags.downloaderUsernameFolder = nest;
    }

    @Test public void onlyATypeRefusalRetriesWithTheOtherVisualKind() {
        assertEquals("video/mp4", LegacyMediaSave.alternateMime("image/jpeg", "REFUSED"));
        assertEquals("image/jpeg", LegacyMediaSave.alternateMime("video/mp4", "REFUSED"));
        // Audio, truncation, HTTP errors, expiry and size limits are final.
        assertNull(LegacyMediaSave.alternateMime("audio/mp4", "REFUSED"));
        assertNull(LegacyMediaSave.alternateMime("image/jpeg", "WRITE_ERROR"));
        assertNull(LegacyMediaSave.alternateMime("video/mp4", "HTTP_ERROR"));
        assertNull(LegacyMediaSave.alternateMime("video/mp4", "EXPIRED"));
        assertNull(LegacyMediaSave.alternateMime("video/mp4", "TOO_LARGE"));
        assertNull(LegacyMediaSave.alternateMime(null, "REFUSED"));
    }

    @Test public void rawFolderTakesPrecedenceOverSafTree() {
        assertFalse(LegacyMediaSave.useInProcessSaf("/storage/emulated/0/IG", "content://tree/x"));
        assertTrue(LegacyMediaSave.useInProcessSaf("", "content://tree/x"));
        assertTrue(LegacyMediaSave.useInProcessSaf("content://tree/x", "content://tree/x"));
        assertFalse(LegacyMediaSave.useInProcessSaf("", ""));
        assertFalse(LegacyMediaSave.useInProcessSaf(null, null));
    }

    @Test public void onlyTheSyncBitReachesTheMuxer() {
        assertEquals(MediaCodec.BUFFER_FLAG_KEY_FRAME, LegacyMediaSave.muxerFlags(MediaExtractor.SAMPLE_FLAG_SYNC));
        assertEquals(0, LegacyMediaSave.muxerFlags(MediaExtractor.SAMPLE_FLAG_ENCRYPTED));
        assertEquals(0, LegacyMediaSave.muxerFlags(MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME));
        assertEquals(MediaCodec.BUFFER_FLAG_KEY_FRAME, LegacyMediaSave.muxerFlags(
                MediaExtractor.SAMPLE_FLAG_SYNC | MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME));
    }

    @Test public void rawPublishNeverOverwritesAnExistingDownload() throws Exception {
        File out = folder.newFolder("downloads");
        FeatureFlags.downloaderCustomPath = out.getAbsolutePath();
        FeatureFlags.downloaderCustomUri = "";
        FeatureFlags.downloaderUsernameFolder = false;
        File existing = new File(out, "user_story_1.jpg");
        Files.write(existing.toPath(), "old".getBytes(StandardCharsets.UTF_8));
        File source = folder.newFile("source.bin");
        Files.write(source.toPath(), "new".getBytes(StandardCharsets.UTF_8));
        Context ctx = ApplicationProvider.getApplicationContext();

        LegacyMediaSave.publish(ctx, source, "user_story_1.jpg", "image/jpeg", "user");

        assertArrayEquals("old".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(existing.toPath()));
        File renamed = new File(out, "user_story_1_1.jpg");
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(renamed.toPath()));
        // No pending temp file is left in the user's folder.
        String[] names = out.list();
        Arrays.sort(names);
        assertArrayEquals(new String[]{"user_story_1.jpg", "user_story_1_1.jpg"}, names);
    }

    @Test public void legacyFetchRefusesADestinationOutsideTheCache() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        File outside = folder.newFile("outside.bin");
        String[] status = new String[1];
        // The engine refuses the main thread, so run like the legacy executors do.
        Thread worker = new Thread(() -> {
            try {
                LegacyMediaSave.fetch(ctx, "https://scontent.cdninstagram.com/v/x.jpg", outside, "image/jpeg");
                status[0] = "SAVED";
            } catch (io.github.somilkhan.instaeclipse2.utils.download.DownloadEngine.Failure failure) {
                status[0] = failure.status;
            }
        });
        worker.start();
        worker.join(10_000);
        // Not a type refusal, so no second attempt; nothing is written outside the cache.
        assertEquals("STORAGE", status[0]);
        assertEquals(0, outside.length());
    }
}
