package io.github.somilkhan.instaeclipse2.mods.media;

import android.content.Context;

import java.io.File;

import io.github.somilkhan.instaeclipse2.utils.download.DownloadEngine;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Fetch and publish for the legacy in-process download buttons (stories, highlights, reels, DMs,
 * profile pictures, share links, comment media, voice messages, Original-quality fallback).
 *
 * <p>These used to open their own HttpURLConnection and copy into a raw MediaStore/file stream:
 * no status or length check, no size cap or host policy, a gallery row that stayed behind when a
 * copy failed, and a raw-path file that silently replaced an existing download of the same name.
 * Both halves now reuse the verified engine: {@link DownloadEngine#fetch} (200 only, announced
 * length, 512 MB cap, Meta CDN policy, signature sniff) and {@link DownloadPublisher}
 * (IS_PENDING rows that are deleted on failure, collision-safe raw files).
 */
final class LegacyMediaSave {
    private LegacyMediaSave() {}

    /**
     * Fetches {@code url} into {@code dest} (an absent or empty file inside the cache directory)
     * and returns the MIME type proven by the bytes. The caller's photo/video guess is often from
     * URL text, so when the server or the bytes prove the other visual kind (status REFUSED, which
     * is decided from the headers and first bytes before the body is read) the fetch is repeated
     * once with that kind. Anything else that is not a verified image/video is refused.
     */
    static String fetch(Context ctx, String url, File dest, String requestedMime) throws DownloadEngine.Failure {
        try {
            return DownloadEngine.fetch(ctx, url, dest, requestedMime, null, null);
        } catch (DownloadEngine.Failure failure) {
            String alternate = alternateMime(requestedMime, failure.status);
            if (alternate == null) throw failure;
            ModuleLog.line("(IE|DL) legacy fetch refused as " + requestedMime + ", retrying as " + alternate);
            return DownloadEngine.fetch(ctx, url, dest, alternate, null, null);
        }
    }

    /** The other visual kind to try after a type refusal, or null when no retry applies. */
    static String alternateMime(String requestedMime, String failureStatus) {
        if (!"REFUSED".equals(failureStatus) || requestedMime == null) return null;
        if (requestedMime.startsWith("image/")) return "video/mp4";
        if (requestedMime.startsWith("video/")) return "image/jpeg";
        return null;
    }

    /**
     * Publishes a complete local file. Same destination order as before: a configured raw folder,
     * else a SAF tree Instagram itself holds, else MediaStore Downloads (or the legacy folder
     * before API 29). Every route removes its own partial output and never overwrites a file.
     */
    static void publish(Context ctx, File source, String filename, String mime, String owner) throws Exception {
        if (useInProcessSaf(FeatureFlags.downloaderCustomPath, FeatureFlags.downloaderCustomUri)) {
            try {
                DownloadPublisher.saveToSaf(ctx, source, filename, mime, FeatureFlags.downloaderCustomUri,
                        owner, FeatureFlags.downloaderUsernameFolder, null);
                return;
            } catch (Exception e) {
                ModuleLog.line("(IE|DL) SAF save failed, falling back to MediaStore: " + e.getClass().getSimpleName());
            }
        }
        DownloadPublisher.save(ctx, source, filename, mime, owner, null);
    }

    /**
     * Extractor sample flags are not codec buffer flags (SAMPLE_FLAG_ENCRYPTED shares its value
     * with BUFFER_FLAG_CODEC_CONFIG, SAMPLE_FLAG_PARTIAL_FRAME with BUFFER_FLAG_END_OF_STREAM);
     * only the sync bit maps across to the muxer.
     */
    static int muxerFlags(int sampleFlags) {
        return (sampleFlags & android.media.MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                ? android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
    }

    /** A raw folder wins; the SAF tree is used only when no raw folder is configured. */
    static boolean useInProcessSaf(String customPath, String customUri) {
        boolean raw = customPath != null && !customPath.isEmpty() && !customPath.startsWith("content://");
        return !raw && customUri != null && !customUri.isEmpty();
    }
}
