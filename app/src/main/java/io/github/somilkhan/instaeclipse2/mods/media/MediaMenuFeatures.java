package io.github.somilkhan.instaeclipse2.mods.media;

import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;

/**
 * Which features add rows to Instagram's post / reel "..." menus. Shared by the hooks that add
 * the rows ({@link PostDownloadContextMenuHook}, {@link ReelDownloadHook}) and the shared click
 * handler, so a row that can be shown is never ignored when clicked.
 */
public final class MediaMenuFeatures {

    private MediaMenuFeatures() {}

    /** Features that add rows to the feed post menu. */
    public static boolean anyPostMenuFeature() {
        return FeatureFlags.enablePostDownload || FeatureFlags.copyMediaLink
                || FeatureFlags.offerCopyImage || FeatureFlags.offerMediaInfo
                || FeatureFlags.offerPlaybackSpeed || FeatureFlags.externalDownloader;
    }

    /** Features that add rows to the reel menu. */
    public static boolean anyReelMenuFeature() {
        return FeatureFlags.enableReelDownload || FeatureFlags.copyMediaLink
                || FeatureFlags.offerMediaInfo || FeatureFlags.offerPlaybackSpeed
                || FeatureFlags.externalDownloader;
    }

    /** Gate for the shared option-click handler, which serves both menus. */
    public static boolean anyMenuFeature() {
        return anyPostMenuFeature() || anyReelMenuFeature();
    }
}
