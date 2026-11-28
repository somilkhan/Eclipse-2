package io.github.somilkhan.instaeclipse2.utils.root;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;
import io.github.somilkhan.instaeclipse2.utils.crash.CrashReportProvider;
import io.github.somilkhan.instaeclipse2.utils.ipc.CompanionBridge;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;
import io.github.somilkhan.instaeclipse2.utils.toast.IeToast;

/**
 * Android side of the root actions: runs {@link RootActions} on the root thread, then does the
 * UI part on the main thread (relaunch, App info, result toast naming the path used).
 */
public final class RootUi {

    private RootUi() {}

    public static RootActions actions(Context ctx) {
        return new RootActions(RootAccess.session(ctx), RootAccess.userId(), CommonUtils.SUPPORTED_PACKAGES);
    }

    /** Installed supported packages; with {@code trustedOnly}, only signer-trusted hosts (file access). */
    public static List<String> installedTargets(Context ctx, boolean trustedOnly) {
        PackageManager pm = ctx.getPackageManager();
        List<String> out = new ArrayList<>();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            if (!trustedOnly || CompanionBridge.isTrustedHost(ctx, pkg)) out.add(pkg);
        }
        return out;
    }

    /**
     * Force stop + relaunch with root. Without root (off, missing, denied or failed) it opens
     * Instagram's App info page, where the Force stop button is, as before.
     */
    public static void restartInstagram(Fragment fragment, String pkg, @Nullable Runnable onDone) {
        Context app = fragment.requireContext().getApplicationContext();
        RootActions actions = actions(app);
        RootAccess.runAsync(() -> actions.forceStop(pkg), outcome -> {
            if (onDone != null) onDone.run();
            boolean root = outcome != null && outcome.path == RootCommands.Path.ROOT && outcome.ok;
            ModuleLog.line("(Root) restart " + pkg + ": " + (root ? "root" : "fallback"));
            if (root) {
                // su returned after am force-stop finished, so the old process is gone.
                Intent launch = app.getPackageManager().getLaunchIntentForPackage(pkg);
                toast(app, app.getString(R.string.root_result_restart_root));
                if (launch != null && fragment.isAdded()) safeStart(fragment, launch);
                return;
            }
            toast(app, withFailure(app, app.getString(R.string.root_result_restart_fallback), outcome));
            if (!fragment.isAdded()) return;
            Intent info = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + pkg))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            safeStart(fragment, info);
        });
    }

    /** Clears the DexKit cache of every installed supported app with root, else explains the in-app way. */
    public static void clearDexKitCache(Fragment fragment, @Nullable Runnable onDone) {
        Context app = fragment.requireContext().getApplicationContext();
        List<String> targets = installedTargets(app, false);
        if (targets.isEmpty()) {
            toast(app, app.getString(R.string.root_result_no_instagram));
            if (onDone != null) onDone.run();
            return;
        }
        RootActions actions = actions(app);
        RootAccess.runAsync(() -> {
            RootActions.Outcome last = null;
            for (String pkg : targets) {
                last = actions.clearDexKitCache(pkg);
                if (last.path != RootCommands.Path.ROOT) break;
            }
            return last;
        }, outcome -> {
            if (onDone != null) onDone.run();
            boolean root = outcome != null && outcome.path == RootCommands.Path.ROOT && outcome.ok;
            toast(app, root ? app.getString(R.string.root_result_dexkit_root)
                    : withFailure(app, app.getString(R.string.root_result_dexkit_fallback), outcome));
        });
    }

    /** Saves crash reports still waiting in Instagram's data dir (root), else explains when they arrive. */
    public static void collectCrashes(Fragment fragment, @Nullable Runnable onDone) {
        Context app = fragment.requireContext().getApplicationContext();
        if (!CrashReportProvider.isEnabled(app)) {
            toast(app, app.getString(R.string.root_result_crashes_off));
            if (onDone != null) onDone.run();
            return;
        }
        List<String> targets = installedTargets(app, true);
        if (targets.isEmpty()) {
            toast(app, app.getString(R.string.root_result_no_instagram));
            if (onDone != null) onDone.run();
            return;
        }
        RootActions actions = actions(app);
        RootAccess.runAsync(() -> {
            int saved = 0;
            boolean allOk = true;
            RootActions.Outcome last = null;
            for (String pkg : targets) {
                last = actions.collectCrashes(pkg, (name, text) -> CrashReportProvider.saveCollected(app, name, text) != null);
                if (last.path != RootCommands.Path.ROOT) return last;
                saved += last.count;
                allOk &= last.ok;
            }
            return new RootActions.Outcome(RootCommands.Path.ROOT, allOk, false, saved);
        }, outcome -> {
            if (onDone != null) onDone.run();
            if (outcome != null && outcome.path == RootCommands.Path.ROOT) {
                toast(app, app.getString(outcome.ok ? R.string.root_result_crashes_root
                        : R.string.root_result_crashes_root_partial, outcome.count));
            } else {
                toast(app, withFailure(app, app.getString(R.string.root_result_crashes_fallback), outcome));
            }
        });
    }

    private static String withFailure(Context ctx, String msg, @Nullable RootActions.Outcome outcome) {
        if (outcome == null || outcome.rootFailed) return msg + " " + ctx.getString(R.string.root_result_failed_suffix);
        return msg;
    }

    private static void toast(Context ctx, String msg) {
        IeToast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    private static void safeStart(Fragment fragment, Intent intent) {
        try {
            fragment.startActivity(intent);
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            ModuleLog.line("(Root) could not open " + intent.getAction() + ": " + e.getMessage());
            toast(fragment.requireContext(), fragment.getString(R.string.ig_toast_no_app_to_open));
        }
    }
}
