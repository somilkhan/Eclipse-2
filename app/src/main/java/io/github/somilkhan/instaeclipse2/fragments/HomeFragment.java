package io.github.somilkhan.instaeclipse2.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;
import io.github.somilkhan.instaeclipse2.utils.core.Contributor;

public class HomeFragment extends Fragment {

    // px/s scroll speed — feels like a gentle conveyor belt
    private static final float SCROLL_SPEED_DP_PER_SEC = 48f;
    /** Carousels rest on the content margin for a moment before drifting. */
    private static final long AUTO_SCROLL_DELAY_MS = 3000L;
    /** Screenshot tests turn the drift off so cards are captured at their resting position. */
    @androidx.annotation.VisibleForTesting
    public static boolean autoScrollEnabled = true;

    private static final String PREFS_NAME = "instaeclipse_cache";
    private static final String PREF_PINNED = "homePinnedShortcuts";
    private static final String DEFAULT_PINNED = "isGhostSeen,isGhostTyping,isGhostStory,isAdBlockEnabled";
    private static final int MAX_PINNED = 8;

    /**
     * Boolean pref keys that represent user-facing features (mirrors the switches in
     * FeaturesFragment). Quick-toggle menu config (quickToggle*), Extreme Mode and derived flags
     * are deliberately excluded so the hero count reflects actual enabled features.
     */
    private static final String[] FEATURE_KEYS = {
            // Ghost mode
            "isGhostSeen", "isGhostTyping", "isGhostStory", "isGhostLive", "allowScreenshots",
            "isGhostScreenshot", "isGhostViewOnce", "permanentViewMode", "keepEphemeralMessages",
            "keepUnsentMessages", "keepEditHistory", "jumpToFirstMessage", "translateMessages", "bulkUnsend", "chatExport",
            // Ads & analytics
            "isAdBlockEnabled", "isAnalyticsBlocked", "disableTrackingLinks", "openLinksExternally",
            // Clean feed
            "hideSuggestionsInFeed", "hideThreadsSuggestions", "hideLikedPosts", "hideCommunityNotes", "followingOnlyFeed",
            "hidePlusUpsells", "expandCaptions",
            // Distraction-free
            "disableStories", "disableFeed", "disableReels", "disableReelsExceptDM", "disableExplore",
            "disableComments", "hideNotesTray", "hideDmBlend", "hideThreadsButton", "readReceiptChats",
            // Navigation bar
            "hideTabHome", "hideTabReels", "hideTabCreate", "hideTabMessages", "hideTabActivity", "hideTabProfile",
            // Misc
            "disableStoryFlipping", "disableVideoAutoPlay", "spoofLastSeen", "disableRepost", "showFollowerToast",
            "showFeatureToasts", "enableStoryMentions", "disableDiscoverPeople", "enableCopyComment",
            "disableDoubleTapLike", "enableCaptionCopy", "enablePhotoZoom", "removeMetaAI",
            "hideShareCreateGroup", "hideFeedShareButton", "hideFeedSaveButton", "hideFeedRepostButton", "hideFeedLikeCount", "disableHighlights", "hideHiddenRequests", "removeEmptyBottomSpace",
            "hideStoryFollowButton", "hideReelsFollowButton", "loopStories", "disableDisappearingSwipe",
            "hideChatCallButtons", "filterStoryTray", "customLikeAnimation", "customStoryRingSize", "resumeReels",
            "externalDownloader", "markReadAfterReply",
            "exactTimestamps", "likeUndo", "profileFollowsYouLabel", "fancyBioFont", "savedOnProfile",
            "storyViewerSearch", "storyLinks", "storyInfo", "commentSearch", "followListExport", "followersHistory",
            // Lock / hide chats
            "lockDirectMessages", "lockWholeApp", "lockSavedCollections", "lockUseFingerprint",
            "hideSpecificChats",
            // Location / theme
            "spoofLocation", "customThemeEnabled", "customFontEnabled", "customEmojiEnabled",
            // Downloader
            "enablePostDownload", "enableStoryDownload", "enableReelDownload", "enableProfileDownload",
            "downloaderUsernameFolder", "downloaderAddTimestamp", "copyMediaLink", "offerAudioDownload",
            "enableVoiceDownload", "enableDmMediaDownload", "offerCopyImage", "offerMediaInfo", "offerPlaybackSpeed",
            "forceReelScrubber", "holdLeftSlowMotion", "maxUploadQuality", "saveInstants", "uploadInstants", "cacheStories",
            // Developer
            "isDevEnabled", "removeBuildExpiredPopup"
    };

    /** Distraction keys FeaturesFragment locks while Extreme Mode is on. */
    private static final List<String> EXTREME_LOCKED = Arrays.asList(
            "disableStories", "disableFeed", "disableReels", "disableReelsExceptDM",
            "disableExplore", "disableComments");

    /** A pinnable toggle on the Home dashboard. */
    private static final class Shortcut {
        final String key;
        final int labelRes;
        final int iconRes;
        final int accent;

        Shortcut(String key, int labelRes, int iconRes, int accent) {
            this.key = key;
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.accent = accent;
        }
    }

    // Same icons / accent colours FeaturesFragment uses for these rows.
    private static final List<Shortcut> CATALOG = Arrays.asList(
            new Shortcut("isGhostSeen", R.string.ig_dialog_ghost_hide_dm_seen, R.drawable.ic_ui_eye_off, 0xFF5E5CE6),
            new Shortcut("isGhostTyping", R.string.ig_dialog_ghost_hide_typing, R.drawable.ic_ui_chat, 0xFF5E5CE6),
            new Shortcut("isGhostStory", R.string.ig_dialog_ghost_hide_story_views, R.drawable.ic_ui_story_ring, 0xFF5E5CE6),
            new Shortcut("isGhostViewOnce", R.string.ig_dialog_ghost_hide_view_once, R.drawable.ic_ui_eye_off, 0xFF5E5CE6),
            new Shortcut("isGhostScreenshot", R.string.ig_dialog_ghost_bypass_screenshot, R.drawable.ic_ui_camera, 0xFF5E5CE6),
            new Shortcut("enablePostDownload", R.string.ig_dialog_downloader_posts, R.drawable.ic_ui_download, 0xFFFF9F0A),
            new Shortcut("enableReelDownload", R.string.ig_dialog_downloader_reels, R.drawable.ic_ui_download, 0xFFFF9F0A),
            new Shortcut("enableStoryDownload", R.string.ig_dialog_downloader_stories, R.drawable.ic_ui_download, 0xFFFF9F0A),
            new Shortcut("isAdBlockEnabled", R.string.ig_dialog_ad_block_ads, R.drawable.ic_ui_shield, 0xFFFF453A),
            new Shortcut("customThemeEnabled", R.string.theme_enable, R.drawable.ic_ui_palette, 0xFFFF375F),
            new Shortcut("disableReels", R.string.ig_dialog_distraction_disable_reels, R.drawable.ic_ui_movie, 0xFF30D158),
            new Shortcut("disableStories", R.string.ig_dialog_distraction_disable_stories, R.drawable.ic_ui_story_ring, 0xFF30D158),
            new Shortcut("disableExplore", R.string.ig_dialog_distraction_disable_explore, R.drawable.ic_ui_search, 0xFF30D158),
            new Shortcut("maxUploadQuality", R.string.ig_dialog_max_upload_quality, R.drawable.ic_ui_upload, 0xFFFF9F0A)
    );

    private SharedPreferences localCache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialButton launchInstagramButton;
    private MaterialButton restartInstagramButton;
    private MaterialCardView instagramStatusCard;
    private TextView instagramStatusText;
    private TextView versionText;
    private TextView instagramVariantText;
    private MaterialButton instagramMultiButton;
    private ImageView instagramLogo;
    private ImageButton instagramInfoIcon;
    private View logoContainer;
    private View statusPill;
    private ImageView statusDot;
    private TextView statusPillText;
    private TextView featuresCountText;
    private LinearLayout shortcutsContainer;
    private TextView shortcutsEmpty;
    private TextView shortcutsHint;

    private String activePackage;
    private List<String> installedPackages;
    private boolean moduleConfirmed;
    private boolean pingReceiverRegistered;

    private AutoScroller contributorsScroller;
    private View compatRow, compatDivider;
    private ImageView compatDot;
    private TextView compatText, compatValue;
    private AutoScroller specialThanksScroller;

    /**
     * Liveness ping: the module answers ACTION_REQUEST_PREFS with ACTION_SEND_PREFS from inside a
     * running Instagram process. A reply proves the module is loaded; no reply just means
     * Instagram isn't running, so we fall back to the plain install state. We only use the reply
     * as a signal — FeaturesFragment remains the one that syncs its contents into the cache.
     */
    private String pingNonce;
    private io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.Pending pingReply;

    private final BroadcastReceiver pingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("io.github.somilkhan.instaeclipse2.ACTION_SEND_PREFS".equals(intent.getAction())
                    && io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.nonceMatches(pingNonce, intent)) {
                moduleConfirmed = true;
                if (isAdded() && getView() != null) updateStatusPill();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        localCache = io.github.somilkhan.instaeclipse2.utils.core.CommonUtils.companionCache(requireContext());

        launchInstagramButton = view.findViewById(R.id.launch_instagram_button);
        restartInstagramButton = view.findViewById(R.id.restart_instagram_button);
        MaterialButton downloadButton = view.findViewById(R.id.download_instagram_button);

        instagramStatusCard = view.findViewById(R.id.instagram_status_card);
        instagramStatusText = view.findViewById(R.id.instagram_status_text);
        versionText = view.findViewById(R.id.home_version_text);
        instagramVariantText = view.findViewById(R.id.instagram_variant_text);
        instagramMultiButton = view.findViewById(R.id.instagram_multi_button);
        instagramLogo = view.findViewById(R.id.instagram_logo);
        instagramInfoIcon = view.findViewById(R.id.instagram_info_icon);
        logoContainer = view.findViewById(R.id.home_logo_container);
        statusPill = view.findViewById(R.id.home_status_pill);
        statusDot = view.findViewById(R.id.home_status_dot);
        statusPillText = view.findViewById(R.id.home_status_pill_text);
        compatRow = view.findViewById(R.id.home_compat_row);
        compatDivider = view.findViewById(R.id.home_compat_divider);
        compatDot = view.findViewById(R.id.home_compat_dot);
        compatText = view.findViewById(R.id.home_compat_text);
        compatValue = view.findViewById(R.id.home_compat_value);
        featuresCountText = view.findViewById(R.id.home_features_count);
        shortcutsContainer = view.findViewById(R.id.home_shortcuts_container);
        shortcutsEmpty = view.findViewById(R.id.home_shortcuts_empty);
        shortcutsHint = view.findViewById(R.id.home_shortcuts_hint);
        ((TextView) view.findViewById(R.id.home_app_version)).setText(getString(R.string.home_status_version, io.github.somilkhan.instaeclipse2.BuildConfig.VERSION_NAME));

        int primary = primary();
        applyTonalSquare(view.findViewById(R.id.home_features_icon_container),
                view.findViewById(R.id.home_features_icon), primary);
        applyTonalSquare(view.findViewById(R.id.home_howto_icon_container),
                view.findViewById(R.id.search_icon), primary);

        View featuresRow = view.findViewById(R.id.home_features_row);
        if (featuresRow != null) featuresRow.setOnClickListener(v -> {
            if (getActivity() instanceof io.github.somilkhan.instaeclipse2.MainActivity) {
                ((io.github.somilkhan.instaeclipse2.MainActivity) getActivity()).selectTab(R.id.nav_features);
            }
        });

        checkInstagramStatus();

        restartInstagramButton.setOnClickListener(v -> restartInstagram());
        downloadButton.setOnClickListener(v -> {
            String url = io.github.somilkhan.instaeclipse2.utils.core.ExportBrand.apkMirrorUrl(
                    io.github.somilkhan.instaeclipse2.utils.core.ExportBrand.STABLE_INSTAGRAM);
            safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        view.findViewById(R.id.home_shortcuts_edit).setOnClickListener(v -> showEditShortcutsDialog());

        setupContributorsAndSpecialThanks(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (contributorsScroller != null) contributorsScroller.start();
        if (specialThanksScroller != null) specialThanksScroller.start();
        // Prefs may have changed in the Features tab or from inside Instagram.
        updateFeatureCount();
        renderShortcuts();
        pingModule();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (contributorsScroller != null) contributorsScroller.stop();
        if (specialThanksScroller != null) specialThanksScroller.stop();
        if (pingReceiverRegistered) {
            if (pingReply != null) { pingReply.cancel(); pingReply = null; }
            pingReceiverRegistered = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contributorsScroller != null) contributorsScroller.stop();
        if (specialThanksScroller != null) specialThanksScroller.stop();
        mainHandler.removeCallbacksAndMessages(null);
    }

    // =========================================================
    // STATUS HERO
    // =========================================================

    private void checkInstagramStatus() {
        PackageManager pm = requireContext().getPackageManager();

        installedPackages = new ArrayList<>();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                installedPackages.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        if (installedPackages.isEmpty()) {
            activePackage = null;
            instagramStatusText.setText(getString(R.string.not_installed_instagram));
            versionText.setText(getString(R.string.home_status_no_version));
            instagramLogo.setImageResource(R.drawable.ic_ui_cancel);
            applyTonalSquare(logoContainer, instagramLogo, colorRes(R.color.ie_error));
            instagramVariantText.setVisibility(View.GONE);
            instagramMultiButton.setVisibility(View.GONE);
            instagramInfoIcon.setVisibility(View.GONE);
            launchInstagramButton.setEnabled(false);
            restartInstagramButton.setEnabled(false);
            bindCompat(null, false);
            updateStatusPill();
            return;
        }

        // Prefer the official package; fall back to first found mod
        activePackage = installedPackages.contains(CommonUtils.IG_PACKAGE_NAME)
                ? CommonUtils.IG_PACKAGE_NAME
                : installedPackages.get(0);

        // Hero identity is just the app name; the install/module state lives in the status row below.
        instagramStatusText.setText(getString(R.string.logging_instagram));
        instagramStatusText.setContentDescription(getString(R.string.installed_instagram_version));
        instagramLogo.setImageResource(R.drawable.ic_app_mark);
        applyTonalSquare(logoContainer, instagramLogo, primary());
        instagramVariantText.setVisibility(View.VISIBLE);
        instagramInfoIcon.setVisibility(View.VISIBLE);

        if (installedPackages.size() > 1) {
            instagramMultiButton.setVisibility(View.VISIBLE);
            instagramMultiButton.setOnClickListener(v -> showDetectedVersionsDialog(pm));
        } else {
            instagramMultiButton.setVisibility(View.GONE);
        }

        bindPackageActions(pm, activePackage);
        updateStatusPill();
    }

    private void bindPackageActions(PackageManager pm, String pkg) {
        activePackage = pkg;

        try {
            String versionName = pm.getPackageInfo(pkg, 0).versionName;
            versionText.setText(getString(R.string.home_status_version, versionName));
            bindCompat(versionName, true);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("");
            bindCompat(null, false);
        }

        instagramVariantText.setText(CommonUtils.getVariantLabel(pkg));

        instagramInfoIcon.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            safeStart(intent);
        });

        launchInstagramButton.setOnClickListener(v -> launchPackage(pkg));
    }

    /** No browser / restricted Settings (work profile, kiosk ROMs) must not crash the app. */
    private void safeStart(Intent intent) {
        if (!isAdded()) return;
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(),
                    getString(R.string.ig_toast_no_app_to_open), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchPackage(String pkg) {
        if (!isAdded()) return;
        Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent != null) {
            safeStart(launchIntent);
        } else {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(), getString(R.string.not_installed_instagram), Toast.LENGTH_SHORT).show();
        }
    }

    /** Compatibility row: tested / untested / alpha / older, with a dialog that explains it. */
    private void bindCompat(@Nullable String versionName, boolean installed) {
        if (compatRow == null) return;
        compatRow.setVisibility(installed ? View.VISIBLE : View.GONE);
        compatDivider.setVisibility(installed ? View.VISIBLE : View.GONE);
        if (!installed) return;

        io.github.somilkhan.instaeclipse2.utils.core.IgCompat.Status st =
                io.github.somilkhan.instaeclipse2.utils.core.IgCompat.classify(versionName);
        // The checked build to offer: alpha users get the checked alpha, others the checked stable build.
        String tested = io.github.somilkhan.instaeclipse2.utils.core.IgCompat.recommendedFor(versionName);
        int label, msg, color;
        switch (st) {
            case TESTED:         label = R.string.home_compat_tested;         msg = R.string.home_compat_msg_tested;         color = colorRes(R.color.ie_ok); break;
            case CHECKED_OFFLINE: label = R.string.home_compat_checked_offline; msg = R.string.home_compat_msg_checked_offline; color = colorRes(R.color.ie_ok); break;
            case UNTESTED_BUILD: label = R.string.home_compat_untested_build; msg = R.string.home_compat_msg_untested_build; color = colorRes(R.color.ie_warn); break;
            case NEWER:          label = R.string.home_compat_newer;          msg = R.string.home_compat_msg_newer;          color = colorRes(R.color.ie_warn); break;
            case NEWER_ALPHA:    label = R.string.home_compat_newer_alpha;    msg = R.string.home_compat_msg_newer_alpha;    color = colorRes(R.color.ie_warn); break;
            case OLDER:          label = R.string.home_compat_older;          msg = R.string.home_compat_msg_older;          color = colorRes(R.color.ie_warn); break;
            default:             label = R.string.home_compat_unknown;        msg = R.string.home_compat_msg_unknown;        color = colorRes(R.color.ie_warn); break;
        }
        compatDot.setImageTintList(ColorStateList.valueOf(color));
        compatText.setText(label);
        boolean ok = io.github.somilkhan.instaeclipse2.utils.core.IgCompat.isVerified(st);
        // Tested: the version is already in the hero, so no trailing value. Otherwise show the target.
        compatValue.setText(ok ? "" : getString(R.string.home_compat_value, tested));
        compatValue.setVisibility(ok ? View.GONE : View.VISIBLE);
        String shown = versionName == null ? "?" : versionName;
        compatRow.setContentDescription(getString(label) + ". " + (ok ? "" : getString(R.string.home_compat_value, tested)));
        compatRow.setOnClickListener(v -> {
            if (!isAdded()) return;
            com.google.android.material.dialog.MaterialAlertDialogBuilder b =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.home_compat_dialog_title, shown))
                            .setMessage(getString(msg, shown, tested, io.github.somilkhan.instaeclipse2.BuildConfig.VERSION_NAME))
                            .setPositiveButton(android.R.string.ok, null);
            if (!ok) b.setNeutralButton(getString(R.string.home_compat_get_tested, tested), (d, w) ->
                    safeStart(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(io.github.somilkhan.instaeclipse2.utils.core.ExportBrand.apkMirrorUrl(tested)))));
            b.show();
        });
    }

    private void updateStatusPill() {
        int color;
        String label;
        if (installedPackages == null || installedPackages.isEmpty()) {
            color = colorRes(R.color.ie_error);
            label = getString(R.string.home_status_not_installed);
        } else if (moduleConfirmed) {
            color = colorRes(R.color.ie_ok);
            label = getString(R.string.home_status_module_active);
        } else {
            color = colorRes(R.color.ie_ok);
            label = getString(R.string.home_status_installed);
        }
        // Grouped-row style: semantic colour only on the status dot, label stays primary text.
        statusDot.setImageTintList(ColorStateList.valueOf(color));
        statusPillText.setTextColor(MaterialColors.getColor(statusPillText, com.google.android.material.R.attr.colorOnSurface));
        statusPillText.setText(label);
    }

    private void updateFeatureCount() {
        if (featuresCountText == null) return;
        int count = 0;
        for (String key : FEATURE_KEYS) {
            if (localCache.getBoolean(key, false)) count++;
        }
        for (String key : io.github.somilkhan.instaeclipse2.utils.feature.ExtrasFeatures.flags()) {
            if (localCache.getBoolean(key, false)) count++;
        }
        featuresCountText.setText(getResources().getQuantityString(R.plurals.home_features_enabled, count, count));
    }

    private void pingModule() {
        if (installedPackages == null || installedPackages.isEmpty() || pingReceiverRegistered) return;
        IntentFilter filter = new IntentFilter("io.github.somilkhan.instaeclipse2.ACTION_SEND_PREFS");
        try {
            // Same registration as FeaturesFragment: the reply comes from the Instagram process.
            if (pingReply != null) pingReply.cancel();
            pingReceiverRegistered = true;
            pingNonce = io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.newNonce();
            pingReply = io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.requestReply(requireContext(),
                    new Intent("io.github.somilkhan.instaeclipse2.ACTION_REQUEST_PREFS").setPackage(activePackage)
                            .putExtra(io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.EXTRA_NONCE, pingNonce),
                    "io.github.somilkhan.instaeclipse2.ACTION_SEND_PREFS", pingReceiver);
        } catch (Throwable ignored) { }
    }

    // =========================================================
    // QUICK ACTIONS
    // =========================================================

    /**
     * FeaturesFragment's "Restart App" only asks the user to kill Instagram manually. Here we
     * first try a root force-stop + relaunch; without root we fall back to that same message and
     * open Instagram's App info page, where the Force stop button lives.
     */
    private void restartInstagram() {
        final String pkg = activePackage;
        if (pkg == null) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(), getString(R.string.not_installed_instagram), Toast.LENGTH_SHORT).show();
            return;
        }
        restartInstagramButton.setEnabled(false);
        new Thread(() -> {
            boolean ok = false;
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + pkg});
                try {
                    p.getOutputStream().close(); // su must not wait on stdin
                    if (p.waitFor(15, TimeUnit.SECONDS)) {
                        ok = p.exitValue() == 0;
                    }
                } finally {
                    // Close the pipes (fd leak otherwise) and never leave a hung su behind.
                    try { p.getInputStream().close(); } catch (Throwable ignored) { }
                    try { p.getErrorStream().close(); } catch (Throwable ignored) { }
                    p.destroy();
                }
            } catch (Throwable ignored) { }
            final boolean rooted = ok;
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                restartInstagramButton.setEnabled(true);
                if (rooted) {
                    moduleConfirmed = false;
                    updateStatusPill();
                    io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(), getString(R.string.home_restart_done), Toast.LENGTH_SHORT).show();
                    mainHandler.postDelayed(() -> launchPackage(pkg), 400);
                } else {
                    io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(), getString(R.string.ig_toast_restart_manual), Toast.LENGTH_LONG).show();
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + pkg));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Throwable ignored) { }
                }
            });
        }, "ie-restart-ig").start();
    }

    // =========================================================
    // QUICK SHORTCUTS
    // =========================================================

    private List<String> getPinnedKeys() {
        String raw = localCache.getString(PREF_PINNED, null);
        if (raw == null) raw = DEFAULT_PINNED;
        List<String> out = new ArrayList<>();
        for (String k : raw.split(",")) {
            String key = k.trim();
            if (!key.isEmpty() && findShortcut(key) != null && !out.contains(key)) out.add(key);
            if (out.size() >= MAX_PINNED) break;
        }
        return out;
    }

    @Nullable
    private static Shortcut findShortcut(String key) {
        for (Shortcut s : CATALOG) if (s.key.equals(key)) return s;
        return null;
    }

    private void renderShortcuts() {
        if (shortcutsContainer == null) return;
        shortcutsContainer.removeAllViews();
        List<String> pinned = getPinnedKeys();
        boolean extreme = localCache.getBoolean("isExtremeMode", false);
        for (String key : pinned) {
            Shortcut s = findShortcut(key);
            if (s == null) continue;
            if (shortcutsContainer.getChildCount() > 0) {
                // Inset hairline aligned with the row titles (grouped-list rhythm).
                View sep = new View(requireContext());
                sep.setBackgroundColor(colorRes(R.color.ie_separator));
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(dp(1) / 2f)));
                slp.setMarginStart(dp(60));
                shortcutsContainer.addView(sep, slp);
            }
            shortcutsContainer.addView(buildShortcutRow(s, extreme && EXTREME_LOCKED.contains(s.key)));
        }
        boolean empty = shortcutsContainer.getChildCount() == 0;
        shortcutsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        shortcutsHint.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private View buildShortcutRow(Shortcut s, boolean locked) {
        Context ctx = requireContext();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));
        row.setPadding(dp(16), dp(6), dp(16), dp(6));
        TypedValue ripple = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setBackgroundResource(ripple.resourceId);

        FrameLayout iconBox = new FrameLayout(ctx);
        ImageView icon = new ImageView(ctx);
        icon.setImageResource(s.iconRes);
        icon.setContentDescription(null);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER);
        iconBox.addView(icon, iconLp);
        applyTonalSquare(iconBox, icon, primary());
        row.addView(iconBox, new LinearLayout.LayoutParams(dp(30), dp(30)));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText(s.labelRes);
        title.setTextAppearance(R.style.TextAppearance_IE_Body);
        title.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface));
        texts.addView(title);
        if (locked) {
            TextView sub = new TextView(ctx);
            sub.setText(R.string.home_shortcuts_locked_extreme);
            sub.setTextAppearance(R.style.TextAppearance_IE_Footnote);
            sub.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurfaceVariant));
            texts.addView(sub);
        }
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(dp(14));
        row.setMinimumHeight(dp(52));
        textLp.setMarginEnd(dp(12));
        row.addView(texts, textLp);

        MaterialSwitch sw = new io.github.somilkhan.instaeclipse2.ui.glass.IosSwitch(ctx);
        sw.setChecked(localCache.getBoolean(s.key, false));
        sw.setContentDescription(getString(s.labelRes));
        row.addView(sw, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (locked) {
            sw.setEnabled(false);
            row.setEnabled(false);
            row.setAlpha(0.4f);
        } else {
            sw.setOnCheckedChangeListener((btn, checked) -> {
                setFeature(s.key, checked);
                updateFeatureCount();
            });
            row.setOnClickListener(v -> sw.toggle());
        }
        return row;
    }

    /**
     * Writes a boolean feature pref exactly like FeaturesFragment does: commit() to the
     * companion's cache, make the file world-readable for XSharedPreferences on a cold IG start,
     * and broadcast ACTION_UPDATE_PREF so a running Instagram applies it live.
     */
    private void setFeature(String key, boolean value) {
        SharedPreferences.Editor editor = localCache.edit();
        editor.putBoolean(key, value);
        // Mirror FeaturesFragment's cascade: turning Reels back on also clears "except in DMs".
        boolean cascade = !value && "disableReels".equals(key)
                && localCache.getBoolean("disableReelsExceptDM", false);
        if (cascade) editor.putBoolean("disableReelsExceptDM", false);
        editor.commit();
        makeLocalCacheWorldReadable();

        sendPrefBroadcast(key, value);
        if (cascade) sendPrefBroadcast("disableReelsExceptDM", false);
    }

    private void sendPrefBroadcast(String key, boolean value) {
        Intent intent = new Intent("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF");
        intent.putExtra("key", key);
        intent.putExtra("value", value);
        io.github.somilkhan.instaeclipse2.utils.core.CommonUtils.broadcastToInstagram(requireContext(), intent);
    }

    private void makeLocalCacheWorldReadable() {
        try {
            java.io.File prefsFile = new java.io.File(
                    requireContext().getApplicationInfo().dataDir + "/shared_prefs/" + PREFS_NAME + ".xml");
            prefsFile.setReadable(true, false);
        } catch (Throwable ignored) { }
    }

    private void showEditShortcutsDialog() {
        final List<String> current = getPinnedKeys();
        final String[] labels = new String[CATALOG.size()];
        final boolean[] checked = new boolean[CATALOG.size()];
        for (int i = 0; i < CATALOG.size(); i++) {
            labels[i] = getString(CATALOG.get(i).labelRes);
            checked[i] = current.contains(CATALOG.get(i).key);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_shortcuts_dialog_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        int count = 0;
                        for (boolean b : checked) if (b) count++;
                        if (count > MAX_PINNED) {
                            checked[which] = false;
                            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(requireContext(),
                                    getString(R.string.home_shortcuts_limit, MAX_PINNED), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Keep the existing order for shortcuts that stay pinned, append new ones.
                    List<String> result = new ArrayList<>();
                    for (String key : current) {
                        int idx = indexOfShortcut(key);
                        if (idx >= 0 && checked[idx]) result.add(key);
                    }
                    for (int i = 0; i < CATALOG.size(); i++) {
                        String key = CATALOG.get(i).key;
                        if (checked[i] && !result.contains(key) && result.size() < MAX_PINNED) result.add(key);
                    }
                    localCache.edit().putString(PREF_PINNED, String.join(",", result)).commit();
                    makeLocalCacheWorldReadable();
                    renderShortcuts();
                })
                .setNegativeButton(R.string.ig_dialog_cancel, null)
                .show();
    }

    private static int indexOfShortcut(String key) {
        for (int i = 0; i < CATALOG.size(); i++) if (CATALOG.get(i).key.equals(key)) return i;
        return -1;
    }

    // =========================================================
    // STYLE HELPERS
    // =========================================================

    /** Settings-style icon tile: soft tint of one accent (or a semantic colour) with a glyph in that colour. */
    private void applyTonalSquare(View container, ImageView icon, int accent) {
        if (container == null) return;
        GradientDrawable bg = new GradientDrawable();
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        bg.setColor((accent & 0x00FFFFFF) | (night ? 0x29000000 : 0x1A000000));
        int w = container.getLayoutParams() != null ? container.getLayoutParams().width : dp(30);
        bg.setCornerRadius(w > 0 ? w * 0.27f : dp(10));
        container.setBackground(bg);
        if (icon != null) icon.setImageTintList(ColorStateList.valueOf(accent));
    }

    private int primary() {
        return MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0xFF5856D6);
    }

    private int colorRes(int res) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), res);
    }

    private int resolveStyle(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.resourceId;
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    // =========================================================
    // CONTRIBUTORS
    // =========================================================

    private void setupContributorsAndSpecialThanks(View rootView) {
        HorizontalScrollView contributorsScroll = rootView.findViewById(R.id.contributors_scroll);
        HorizontalScrollView specialThanksScroll = rootView.findViewById(R.id.special_thanks_scroll);
        LinearLayout contributorsContainer = rootView.findViewById(R.id.contributors_container);
        LinearLayout specialThanksContainer = rootView.findViewById(R.id.special_thanks_container);

        List<Contributor> contributors = Arrays.asList(
                // Eclipse 2 maintainer, then the original InstaEclipse author.
                new Contributor("somilkhan", "https://github.com/somilkhan", null, null),
                new Contributor("ReSo7200", "https://github.com/ReSo7200", "https://linkedin.com/in/abdalhaleem-altamimi", null),
                new Contributor("swakwork", "https://github.com/swakwork", null, null),
                new Contributor("isma3iloiso", "https://github.com/isma3iloiso", null, null),
                new Contributor("Placeholder6", "https://github.com/Placeholder6", null, null),
                new Contributor("frknkrc44", "https://github.com/frknkrc44", null, null),
                new Contributor("BrianML", "https://github.com/brianml31", null, "https://t.me/instamoon_channel"),
                new Contributor("silvzr", "https://github.com/silvzr", null, null),
                new Contributor("oct", "https://github.com/oct888", null, null),
                new Contributor("HalfManBear", "https://github.com/halfmanbear", null, null),
                new Contributor("ar5to", "https://github.com/ar5to", null, "https://t.me/ar5to"),
                new Contributor("particle-box", "https://github.com/particle-box", null, null),
                new Contributor("rsr", null, null, "https://t.me/rsr1337"),
                // Merged-PR authors that were missing from this list.
                new Contributor("akifakif32", "https://github.com/akifakif32", null, null),
                new Contributor("HackZy01", "https://github.com/HackZy01", null, null),
                new Contributor("Xiddoc", "https://github.com/Xiddoc", null, null),
                new Contributor("GlitchDaBest", "https://github.com/GlitchDaBest", null, null),
                new Contributor("NicKz101", "https://github.com/NicKz101", null, null),
                new Contributor("HcNguyen111", "https://github.com/HcNguyen111", null, null),
                new Contributor("Lxchoooo", "https://github.com/Lxchoooo", null, null),
                new Contributor("EscapeA", "https://github.com/EscapeA", null, null),
                new Contributor("Figim", "https://github.com/Figim", null, null),
                new Contributor("oka1da", "https://github.com/oka1da", null, null),
                new Contributor("uurcan7", "https://github.com/uurcan7", null, null),
                new Contributor("zarzet", "https://github.com/zarzet", null, null),
                new Contributor("xxOrdulu52xx", "https://github.com/xxOrdulu52xx", null, null),
                new Contributor("dpwbusr", "https://github.com/dpwbusr", null, null),
                new Contributor("d9k6s", "https://github.com/d9k6s", null, null),
                new Contributor("izadiegizabal", "https://github.com/izadiegizabal", null, null),
                new Contributor("b4lol", "https://github.com/b4lol", null, null)
        );

        List<Contributor> specialThanks = Arrays.asList(
                new Contributor("xHookman", "https://github.com/xHookman", null, null),
                new Contributor("Bluepapilte", null, null, "https://t.me/instasmashrepo"),
                new Contributor("BdrcnAYYDIN", null, null, "https://t.me/BdrcnAYYDIN"),
                new Contributor("Amàzing World", null, null, null)
        );

        // Inflate each list twice so the loop restarts seamlessly
        inflateCards(contributors, contributorsContainer);
        inflateCards(contributors, contributorsContainer);
        inflateCards(specialThanks, specialThanksContainer);
        inflateCards(specialThanks, specialThanksContainer);

        // Start infinite scroll once layout is complete
        contributorsContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        contributorsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int halfWidth = contributorsContainer.getWidth() / 2;
                        contributorsScroller = new AutoScroller(contributorsScroll, halfWidth);
                        contributorsScroller.attachTouchPause();
                        if (isResumed()) contributorsScroller.start();
                    }
                });

        specialThanksContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        specialThanksContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int halfWidth = specialThanksContainer.getWidth() / 2;
                        specialThanksScroller = new AutoScroller(specialThanksScroll, halfWidth);
                        specialThanksScroller.attachTouchPause();
                        if (isResumed()) specialThanksScroller.start();
                    }
                });
    }

    /**
     * Continuous, seamless loop scroller for a duplicated-content HorizontalScrollView. It advances
     * the scroll by a fixed speed every animation frame, reading the LIVE scroll position and
     * wrapping it modulo one copy's width — so after a user drag it simply continues from wherever
     * the finger left off (the content is duplicated, so the wrap is invisible). Momentum fling is
     * disabled on the view (LoopScrollView), which is what removes the old snap-back / random jump.
     */
    private final class AutoScroller implements android.view.Choreographer.FrameCallback {
        /** Off-screen (scrolled away, other tab, screen off): check again at this low rate. */
        private static final long OFFSCREEN_POLL_MS = 300L;
        private final HorizontalScrollView sv;
        private final int halfWidth;
        private final float stepPxPerNs;
        private final android.graphics.Rect visible = new android.graphics.Rect();
        private boolean running;
        private boolean paused;
        private long lastFrameNs;
        /** Exact (sub-pixel) loop position; scrollX is its integer part. */
        private float pos = -1f;

        AutoScroller(HorizontalScrollView sv, int halfWidth) {
            this.sv = sv;
            this.halfWidth = halfWidth;
            this.stepPxPerNs = SCROLL_SPEED_DP_PER_SEC * getResources().getDisplayMetrics().density / 1e9f;
        }

        void start() {
            if (running || halfWidth <= 0 || !autoScrollEnabled) return;
            // Reduce motion / TalkBack: the strip stays still and scrolls only by hand.
            if (io.github.somilkhan.instaeclipse2.ui.glass.Motion.reduceMotion() || touchExploring()) return;
            running = true;
            lastFrameNs = 0;
            pos = -1f;
            android.view.Choreographer.getInstance().postFrameCallbackDelayed(this,
                    sv.getScrollX() == 0 ? AUTO_SCROLL_DELAY_MS : 0L);
        }

        private boolean touchExploring() {
            android.view.accessibility.AccessibilityManager am = (android.view.accessibility.AccessibilityManager)
                    sv.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            return am != null && am.isTouchExplorationEnabled();
        }

        void stop() {
            running = false;
            android.view.Choreographer.getInstance().removeFrameCallback(this);
            setFraction(0f);
        }

        private void setFraction(float f) {
            View child = sv.getChildCount() > 0 ? sv.getChildAt(0) : null;
            if (child != null && child.getTranslationX() != -f) child.setTranslationX(-f);
        }

        @Override
        public void doFrame(long now) {
            if (!running) return;
            android.view.Choreographer ch = android.view.Choreographer.getInstance();
            // Nothing to animate while the strip is off screen: don't keep the app drawing at the
            // display's full refresh rate (120 Hz on most phones) for an invisible carousel.
            if (!sv.isShown() || !sv.getGlobalVisibleRect(visible)) {
                lastFrameNs = 0;
                ch.postFrameCallbackDelayed(this, OFFSCREEN_POLL_MS);
                return;
            }
            if (lastFrameNs == 0) lastFrameNs = now;
            // Clamp so a long pause (GC, background) doesn't make the strip jump.
            long dt = Math.min(now - lastFrameNs, 50_000_000L);
            lastFrameNs = now;
            if (!paused && halfWidth > 0) {
                if (pos < 0f || Math.abs(pos - sv.getScrollX()) >= 1f) pos = sv.getScrollX();
                pos = (pos + stepPxPerNs * dt) % halfWidth;
                int x = (int) pos;
                if (sv.getScrollX() != x) sv.scrollTo(x, 0);
                // Sub-pixel remainder as a translation: a constant-speed glide instead of the
                // 0/1/2 px steps integer scrolling produces at high refresh rates.
                setFraction(pos - x);
            }
            ch.postFrameCallback(this);
        }

        @SuppressLint("ClickableViewAccessibility")
        void attachTouchPause() {
            sv.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        paused = true;      // let the user drag freely
                        setFraction(0f);
                        pos = -1f;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        paused = false;     // resume from the live position (no fling to fight)
                        lastFrameNs = 0;
                        pos = -1f;
                        break;
                }
                return false; // keep normal drag handling
            });
        }
    }

    private void inflateCards(List<Contributor> list, LinearLayout container) {
        for (Contributor c : list) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.contributor_card, container, false);
            setupContributorCard(v, c);
            container.addView(v);
        }
    }

    private void setupContributorCard(View view, Contributor contributor) {
        TextView nameTextView = view.findViewById(R.id.contributor_name);
        nameTextView.setText(contributor.name());

        // Monogram avatar: first letter on a per-name coloured circle.
        TextView avatar = view.findViewById(R.id.contributor_avatar);
        if (avatar != null) {
            String name = contributor.name() == null ? "" : contributor.name().trim();
            String initial = "?";
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (Character.isLetterOrDigit(ch)) { initial = String.valueOf(Character.toUpperCase(ch)); break; }
            }
            // Neutral monogram (one-accent palette): tertiary fill + secondary-label initial.
            int color = colorRes(R.color.ie_fill_tertiary);
            avatar.setTextColor(colorRes(R.color.ie_on_surface_variant));
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(color);
            avatar.setText(initial);
            avatar.setBackground(circle);
        }

        // Overlay the GitHub profile photo when the contributor has a GitHub link — github.com/<user>.png
        // resolves to their avatar. On no-link/failure the monogram above stays visible.
        ImageView avatarImg = view.findViewById(R.id.contributor_avatar_img);
        if (avatarImg != null) {
            avatarImg.setVisibility(View.GONE); // reset (fresh inflate, but be safe)
            String gh = contributor.githubUrl();
            if (gh != null && !gh.trim().isEmpty()) {
                String u = gh.trim();
                if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
                io.github.somilkhan.instaeclipse2.utils.ui.AvatarLoader.loadCircular(u + ".png?size=144", avatarImg);
            }
        }

        ImageButton githubButton = view.findViewById(R.id.github_button);
        if (contributor.githubUrl() != null) {
            githubButton.setVisibility(View.VISIBLE);
            githubButton.setOnClickListener(v -> openLink(contributor.githubUrl()));
        } else {
            githubButton.setVisibility(View.GONE);
        }

        ImageButton linkedinButton = view.findViewById(R.id.linkedin_button);
        if (contributor.linkedinUrl() != null) {
            linkedinButton.setVisibility(View.VISIBLE);
            linkedinButton.setOnClickListener(v -> openLink(contributor.linkedinUrl()));
        } else {
            linkedinButton.setVisibility(View.GONE);
        }

        ImageButton telegramButton = view.findViewById(R.id.telegram_button);
        if (contributor.telegramUrl() != null) {
            telegramButton.setVisibility(View.VISIBLE);
            telegramButton.setOnClickListener(v -> openLink(contributor.telegramUrl()));
        } else {
            telegramButton.setVisibility(View.GONE);
        }
    }

    private void showDetectedVersionsDialog(PackageManager pm) {
        String[] labels = new String[installedPackages.size()];
        for (int i = 0; i < installedPackages.size(); i++) {
            String pkg = installedPackages.get(i);
            String version = "?";
            try { version = pm.getPackageInfo(pkg, 0).versionName; }
            catch (PackageManager.NameNotFoundException ignored) { }
            labels[i] = CommonUtils.getVariantLabel(pkg) + "  —  v" + version;
        }

        int currentIndex = installedPackages.indexOf(activePackage);
        final int[] selectedIndex = {currentIndex};

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Detected versions")
                .setSingleChoiceItems(labels, currentIndex, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Use this", (dialog, which) -> {
                    String chosen = installedPackages.get(selectedIndex[0]);
                    if (!chosen.equals(activePackage)) {
                        bindPackageActions(pm, chosen);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openLink(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
