package io.github.somilkhan.instaeclipse2.utils.ghost;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.mods.ghost.ui.GhostEmojiManager;
import io.github.somilkhan.instaeclipse2.mods.ui.UIHookManager;
import io.github.somilkhan.instaeclipse2.utils.core.SettingsManager;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.i18n.I18n;

public class GhostModeUtils {
    public static boolean isGhostModeActive() {
        if (FeatureFlags.quickToggleSeen && FeatureFlags.isGhostSeen) return true;
        if (FeatureFlags.quickToggleTyping && FeatureFlags.isGhostTyping) return true;
        if (FeatureFlags.quickToggleScreenshot && FeatureFlags.isGhostScreenshot) return true;
        if (FeatureFlags.quickToggleViewOnce && FeatureFlags.isGhostViewOnce) return true;
        if (FeatureFlags.quickToggleStory && FeatureFlags.isGhostStory) return true;
        if (FeatureFlags.quickToggleLive && FeatureFlags.isGhostLive) return true;
        if (FeatureFlags.quickToggleEphemeral && FeatureFlags.keepEphemeralMessages) return true;
        if (FeatureFlags.quickTogglePermanentView && FeatureFlags.permanentViewMode) return true;
        return FeatureFlags.quickToggleAllowScreenshots && FeatureFlags.allowScreenshots;
    }


    public static void toggleSelectedGhostOptions(Context context) {
        boolean anySelected = false;
        boolean shouldDisable = false;

        if (FeatureFlags.quickToggleSeen) {
            anySelected = true;
            if (FeatureFlags.isGhostSeen) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleTyping) {
            anySelected = true;
            if (FeatureFlags.isGhostTyping) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleScreenshot) {
            anySelected = true;
            if (FeatureFlags.isGhostScreenshot) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleViewOnce) {
            anySelected = true;
            if (FeatureFlags.isGhostViewOnce) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleStory) {
            anySelected = true;
            if (FeatureFlags.isGhostStory) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleLive) {
            anySelected = true;
            if (FeatureFlags.isGhostLive) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleEphemeral) {
            anySelected = true;
            if (FeatureFlags.keepEphemeralMessages) shouldDisable = true;
        }
        if (FeatureFlags.quickTogglePermanentView) {
            anySelected = true;
            if (FeatureFlags.permanentViewMode) shouldDisable = true;
        }
        if (FeatureFlags.quickToggleAllowScreenshots) {
            anySelected = true;
            if (FeatureFlags.allowScreenshots) shouldDisable = true;
        }

        if (!anySelected) {
            Activity activity = UIHookManager.getCurrentActivity();
            if (activity != null) {
                GhostEmojiManager.addGhostEmojiNextToInbox(activity, false);
            }
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(context, "❗ " + I18n.t(context, R.string.ig_toast_ghost_no_options), Toast.LENGTH_SHORT).show();
            return; // Nothing to do
        }

        boolean newState = !shouldDisable; // true = enable, false = disable

        if (FeatureFlags.quickToggleSeen) FeatureFlags.isGhostSeen = newState;
        if (FeatureFlags.quickToggleTyping) FeatureFlags.isGhostTyping = newState;
        if (FeatureFlags.quickToggleScreenshot) FeatureFlags.isGhostScreenshot = newState;
        if (FeatureFlags.quickToggleViewOnce) FeatureFlags.isGhostViewOnce = newState;
        if (FeatureFlags.quickToggleStory) FeatureFlags.isGhostStory = newState;
        if (FeatureFlags.quickToggleLive) FeatureFlags.isGhostLive = newState;
        if (FeatureFlags.quickToggleEphemeral) FeatureFlags.keepEphemeralMessages = newState;
        if (FeatureFlags.quickTogglePermanentView) FeatureFlags.permanentViewMode = newState;
        if (FeatureFlags.quickToggleAllowScreenshots) FeatureFlags.allowScreenshots = newState;

        // Save changes
        SettingsManager.saveAllFlags();

        Activity activity = UIHookManager.getCurrentActivity();
        if (activity != null) {
            GhostEmojiManager.addGhostEmojiNextToInbox(activity, newState); // true = show ghost, false = hide
        }


        // Toast
        if (newState) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(context, "👻 " + I18n.t(context, R.string.ig_toast_ghost_enabled), Toast.LENGTH_SHORT).show();
        } else {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(context, "❌ " + I18n.t(context, R.string.ig_toast_ghost_disabled), Toast.LENGTH_SHORT).show();
        }
    }
}
