package io.github.somilkhan.instaeclipse2.Xposed;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import org.luckypray.dexkit.DexKitBridge;

import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.somilkhan.instaeclipse2.mods.ads.AdBlocker;
import io.github.somilkhan.instaeclipse2.mods.feed.FeedPhotoZoomHook;
import io.github.somilkhan.instaeclipse2.mods.location.LocationSpoofHook;
import io.github.somilkhan.instaeclipse2.utils.log.Logging;
import io.github.somilkhan.instaeclipse2.mods.media.ForceReelQualityHook;
import io.github.somilkhan.instaeclipse2.mods.feed.HideSuggestedFeedItemsHook;
import io.github.somilkhan.instaeclipse2.mods.ads.TrackingLinkDisable;
import io.github.somilkhan.instaeclipse2.mods.devops.BuildExpiredPopupHook;
import io.github.somilkhan.instaeclipse2.mods.devops.DevOptionsUnlockHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostChannelMarkAsReadHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostDMMarkAsReadHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostDMSeenHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostEphemeralKeepHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostPermanentViewHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.ViewOnceBadgeHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.KeepUnsentMessagesHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostScreenshotDetectionHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostStorySeenHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostTypingIndicatorHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.GhostViewOnceHook;
import io.github.somilkhan.instaeclipse2.mods.ghost.ScreenshotPermissionHook;
import io.github.somilkhan.instaeclipse2.mods.media.FeedVideoDownloadHook;
import io.github.somilkhan.instaeclipse2.mods.media.PostDownloadContextMenuHook;
import io.github.somilkhan.instaeclipse2.mods.media.ProfilePicDownloadHook;
import io.github.somilkhan.instaeclipse2.mods.media.ReelDownloadHook;
import io.github.somilkhan.instaeclipse2.mods.media.StoryDownloadHook;
import io.github.somilkhan.instaeclipse2.mods.misc.CommentCopyHook;
import io.github.somilkhan.instaeclipse2.mods.misc.CaptionCopyContextMenuHook;
import io.github.somilkhan.instaeclipse2.mods.misc.DisableDoubleTapLikeHook;
import io.github.somilkhan.instaeclipse2.mods.misc.DisableStoryFlippingHook;
import io.github.somilkhan.instaeclipse2.mods.misc.DisableVideoAutoPlayHook;
import io.github.somilkhan.instaeclipse2.mods.misc.StoryMentionHook;
import io.github.somilkhan.instaeclipse2.mods.network.IGNetworkInterceptor;
import io.github.somilkhan.instaeclipse2.mods.ui.UIHookManager;
import io.github.somilkhan.instaeclipse2.mods.ui.theme.IgThemeHook;
import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;
import io.github.somilkhan.instaeclipse2.utils.core.DexKitCache;
import io.github.somilkhan.instaeclipse2.utils.core.HookInstallCoordinator;
import io.github.somilkhan.instaeclipse2.utils.core.SettingsManager;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureManager;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;


@SuppressLint("UnsafeDynamicallyLoadedCode")
public class Module implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    // List of supported Instagram package names (maintained in CommonUtils)
    private static final List<String> SUPPORTED_PACKAGES = CommonUtils.SUPPORTED_PACKAGES;
    /** Prevent duplicate receivers/callbacks if Application.attach is observed more than once. */
    private static final java.util.concurrent.atomic.AtomicBoolean ATTACH_INITIALIZED =
            new java.util.concurrent.atomic.AtomicBoolean();
    /** Process-local: a new Instagram process creates a fresh coordinator and reinstalls hooks. */
    private static final HookInstallCoordinator INSTALL_COORDINATOR = new HookInstallCoordinator();
    public static DexKitBridge dexKitBridge;
    public static ClassLoader hostClassLoader;
    public static String moduleSourceDir;
    private static String moduleLibDir;

    // for dev usage
    /*
    public static void showToast(final String text) {
        new Handler(Looper.getMainLooper()).post(() -> io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(AndroidAppHelper.currentApplication().getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }
    */

    @Override
    public void initZygote(StartupParam startupParam) {
        moduleSourceDir = startupParam.modulePath;

        String abi = Build.SUPPORTED_ABIS[0];
        String abiFolder;
        if (abi.equalsIgnoreCase("arm64-v8a")) abiFolder = "arm64";
        else if (abi.equalsIgnoreCase("armeabi-v7a") || abi.equalsIgnoreCase("armeabi") || abi.equalsIgnoreCase("armv8i"))
            abiFolder = "arm";
        else if (abi.equalsIgnoreCase("x86")) abiFolder = "x86";
        else if (abi.equalsIgnoreCase("x86_64")) abiFolder = "x86_64";
        else abiFolder = abi;

        moduleLibDir = moduleSourceDir.substring(0, moduleSourceDir.lastIndexOf("/")) + "/lib/" + abiFolder;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // Ensure preferences are loaded


        // Hook into Instagram and its clones
        // Debug kill switch: `adb shell setprop debug.instaeclipse.off 1` loads the module but
        // installs no hooks (isolates crashes to the framework vs. this module).
        if (SUPPORTED_PACKAGES.contains(lpparam.packageName) && "1".equals(debugProp("debug.instaeclipse.off"))) {
            android.util.Log.i("InstaEclipse", "debug.instaeclipse.off=1 -> no hooks installed");
            return;
        }
        if (SUPPORTED_PACKAGES.contains(lpparam.packageName)) {
            // Only the main app process: Instagram's helper processes (":fbns" push, browser,
            // video …) never show our features, so hooking them only costs startup time and memory.
            if (lpparam.processName != null && !lpparam.processName.equals(lpparam.packageName)) {
                return;
            }
            // Earliest point: catches crashes during our own hook setup too.
            io.github.somilkhan.instaeclipse2.utils.crash.CrashReporter.install(lpparam.packageName);
            try {
                if (dexKitBridge == null) {
                    // Load the .so file from your module (if not already loaded)
                    System.load(moduleLibDir + "/libdexkit.so");
                    // ModuleLog.line("libdexkit.so loaded successfully.");

                    // Initialize DexKitBridge with the target app's APK
                    dexKitBridge = DexKitBridge.create(lpparam.appInfo.sourceDir);
                    // 457+: strings moved into shared switch tables; StringTables reads them from the APK.
                    io.github.somilkhan.instaeclipse2.utils.core.StringTables.init(lpparam.appInfo.sourceDir);
                    // ModuleLog.line("DexKitBridge initialized with target APK: " + lpparam.appInfo.sourceDir);
                }

                // Use the target app's ClassLoader
                hostClassLoader = lpparam.classLoader;

                // Call the method to hook the target app
                hookInstagram(lpparam);

            } catch (UnsatisfiedLinkError e) {
                // An Error, not an Exception: e.g. a native lib the kernel's page size can't map
                // (16 KB-page devices need 16 KB-aligned segments). Log it instead of failing silently.
                ModuleLog.line("(InstaEclipse): ❌ libdexkit.so failed to load for " + lpparam.packageName
                        + " (no features will work): " + e);
            } catch (Throwable e) {
                ModuleLog.line("(InstaEclipse): ❌ Failed to initialize DexKitBridge for " + lpparam.packageName + ": " + e);
            }
        }
    }

    /** Identifies this exact module APK: versionCode plus the APK's size and mtime. */
    static String moduleBuildId() {
        String id = String.valueOf(io.github.somilkhan.instaeclipse2.BuildConfig.VERSION_CODE);
        try {
            java.io.File apk = new java.io.File(moduleSourceDir);
            if (apk.isFile()) id += "-" + apk.length() + "-" + apk.lastModified();
        } catch (Throwable ignored) {
        }
        return id;
    }

    /** First line of every Instagram session, so a shared log always says which build wrote it. */
    private static void logSessionBanner(Context context) {
        String ig = "?";
        try {
            android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ig = pi.versionName + " (" + pi.getLongVersionCode() + ")";
        } catch (Throwable ignored) {
        }
        ModuleLog.line("(Eclipse 2) session start: module "
                + io.github.somilkhan.instaeclipse2.BuildConfig.VERSION_NAME + " ("
                + io.github.somilkhan.instaeclipse2.BuildConfig.VERSION_CODE + "), "
                + context.getPackageName() + " " + ig);
    }

    /** Discovery sweep reached its end normally; this is not a persistent hook-installed flag. */
    private static final String CACHE_COMPLETE_KEY = "InstallSweepReachedEnd_v2";

    private static java.util.Set<String> skipped;

    /** Debug bisect switch: `setprop debug.instaeclipse.skip A,B` skips those hook installs. */
    static boolean skip(String name) {
        if (skipped == null) {
            skipped = new java.util.HashSet<>();
            for (String prop : new String[]{"skip", "skip2", "skip3", "skip4", "skip5", "skip6"}) // props max 91 chars each
                for (String k : debugProp("debug.instaeclipse." + prop).split(",")) if (!k.trim().isEmpty()) skipped.add(k.trim());
            if (!skipped.isEmpty()) ModuleLog.line("(InstaEclipse) debug skip: " + skipped);
        }
        return skipped.contains(name);
    }

    /**
     * Installs one hook unless skipped ("Hook" or "Hook.part"). Each install gets its own try,
     * so a throw only loses that hook and the log names it with the cause.
     */
    private static void isolatedInstall(String name, Runnable install) {
        int dot = name.indexOf('.');
        if (skip(name) || (dot > 0 && skip(name.substring(0, dot)))) return;
        try {
            install.run();
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Install): ❌ " + name + ": " + t);
        }
    }

    /** Reads a system property (debug switches); empty string when unavailable. */
    static String debugProp(String key) {
        try {
            Object v = Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
            return v instanceof String ? (String) v : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private void hookInstagram(XC_LoadPackage.LoadPackageParam lpparam) {

        try {


            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);
                    // Install CommentCopyButtonHook BEFORE Instagram's Application.attach() runs
                    // so we catch any ViewBinding pre-inflation that happens during attach()
                    Context context = (Context) param.args[0];
                    SettingsManager.init(context);
                    SettingsManager.loadAllFlags(context);

                    // Init DexKit cache — checks IG version to decide if saved descriptors are valid.
                    // Must run before any hook that calls DexKitCache.isCacheValid().
                    try {
                        android.content.pm.PackageInfo pi =
                                context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                        long vc = pi.getLongVersionCode();
                        // Key = Instagram build + this exact module APK. Keyed on Instagram alone, a
                        // module update kept replaying descriptors resolved by the older lookup code
                        // (and debug rebuilds share one versionCode), so fixed hooks still failed.
                        DexKitCache.init(context, String.valueOf(vc) + "|" + moduleBuildId());
                    } catch (Throwable e) {
                        ModuleLog.line("(DexKitCache) ❌ init failed: " + e.getMessage());
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) { io.github.somilkhan.instaeclipse2.utils.core.JitGuard.tick(param.method);

                    if (!ATTACH_INITIALIZED.compareAndSet(false, true)) {
                        android.util.Log.d("InstaEclipse", "Duplicate Application.attach ignored");
                        return;
                    }

                    // Setup context, preferences
                    Context context = (Context) param.args[0];
                    SettingsManager.init(context);
                    // Settings changed in the companion while Instagram wasn't running.
                    io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.pull(context);
                    SettingsManager.loadAllFlags(context);
                    // ...and the other way: bring the companion's copy up to date with changes
                    // made in this menu (newer side wins per key, see PrefsMerge).
                    io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.pushAsync(context);
                    // ...and while it sat frozen in the background: check again on every resume.
                    if (param.thisObject instanceof android.app.Application) {
                        io.github.somilkhan.instaeclipse2.utils.ghost.CurrentConversation.registerApplication((android.app.Application) param.thisObject);
                        ((android.app.Application) param.thisObject).registerActivityLifecycleCallbacks(
                                new android.app.Application.ActivityLifecycleCallbacks() {
                                    @Override public void onActivityResumed(android.app.Activity a) {
                                        io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.pullAsync(a);
                                    }
                                    @Override public void onActivityCreated(android.app.Activity a, Bundle b) {}
                                    @Override public void onActivityStarted(android.app.Activity a) {}
                                    @Override public void onActivityPaused(android.app.Activity a) {}
                                    @Override public void onActivityStopped(android.app.Activity a) {}
                                    @Override public void onActivitySaveInstanceState(android.app.Activity a, Bundle b) {}
                                    @Override public void onActivityDestroyed(android.app.Activity a) {}
                                });
                    }

                    // In-app log viewer: every ModuleLog.line(...) call across the hook codebase
                    // appends to this buffer, which the companion app can read via IPC.
                    // Own file name: the original InstaEclipse (ps.reso.instaeclipse) and older
                    // forks write "instaeclipse_module.log" in this same Instagram data dir, and
                    // reading that history mixed their stale failures into our log viewer.
                    Logging.init(context, "eclipse2_module.log");
                    logSessionBanner(context);
                    LegacyModuleCheck.schedule(context);
                    // Mirror the buffer into a file owned by the companion app (LogMirrorProvider)
                    // so its Logs tab works even when Instagram is backgrounded or killed.
                    Logging.enableMirror(context);
                    // Hand pending/previous-session crash reports to the companion (saved to the chosen folder).
                    io.github.somilkhan.instaeclipse2.utils.crash.CrashReporter.onAppReady(context);

                    // Pull downloader path from companion app's cache so it's available even
                    // when Instagram was started without ever receiving the sync broadcast.
                    try {
                        XSharedPreferences cp = new XSharedPreferences(CommonUtils.MY_PACKAGE_NAME, "instaeclipse_cache");
                        cp.reload();
                        String path = cp.getString("downloaderCustomPath", "");
                        String uri  = cp.getString("downloaderCustomUri",  "");
                        if (!path.isEmpty()) FeatureFlags.downloaderCustomPath = path;
                        if (!uri.isEmpty())  FeatureFlags.downloaderCustomUri  = uri;
                    } catch (Throwable ignored) {
                    }

                    FeatureManager.refreshFeatureStatus(); // Update internal feature states

                    // Activate the LSPosed Sync Bridge to listen to FeaturesFragment updates
                    registerSyncReceiver(context);

                    // Startup tab must hook framework activity creation before Instagram creates
                    // its first Activity. It is reflection-only and cheap enough for this phase.
                    isolatedInstall("StartupTabHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.StartupTabHook().install(hostClassLoader));
                    // Custom Like uses direct class/method reflection, not DexKit. Install it
                    // before the delayed, sequential discovery sweep so it is ready as soon as
                    // Instagram reaches the feed (especially on a cold/version-change launch).
                    isolatedInstall("LikeAnimationHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.LikeAnimationHook().install());
                    // Copy Username is also reflection-only; install its lifecycle trigger early.
                    isolatedInstall("CopyProfileUsernameHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.CopyProfileUsernameHook().install(hostClassLoader));

                    try {
                        UIHookManager.registerConfigImportReceiver(context);
                    } catch (Throwable e) {
                        ModuleLog.line("(InstaEclipse | ImportReceiver): ❌ " + e.getMessage());
                    }
                    try {
                        UIHookManager.registerSettingsRestoreReceiver(context);
                    } catch (Throwable e) {
                        ModuleLog.line("(InstaEclipse | RestoreReceiver): ❌ " + e.getMessage());
                    }
                    final Runnable installFeatureHooks = () -> {
                    // Resolve renamed model classes (User is X.02gr on 458) before any hook asks.
                    try {
                        io.github.somilkhan.instaeclipse2.utils.core.IgModels.ensure(dexKitBridge, hostClassLoader);
                    } catch (Throwable e) {
                        ModuleLog.line("(IE|Models) ❌ " + e);
                    }
                    UIHookManager instagramUI = new UIHookManager();
                    if (!skip("instagramUI")) instagramUI.mainActivity(hostClassLoader);

                    IGNetworkInterceptor interceptor = new IGNetworkInterceptor();

                    // --- Feature Hooks ---

                    // Developer Options
                    try {
                        if (!skip("DevOptionsUnlockHook")) new DevOptionsUnlockHook().handleDevOptions(dexKitBridge);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | DevOptions): ❌ Failed to hook");
                    }

                    // Ghost Mode
                    isolatedInstall("GhostDMSeenHook", () -> new GhostDMSeenHook().handleSeenBlock(dexKitBridge)); // DM Seen
                    isolatedInstall("GhostDMMarkAsReadHook", () -> new GhostDMMarkAsReadHook(moduleSourceDir).install(lpparam.classLoader)); // Mark as Read Button
                    isolatedInstall("GhostChannelMarkAsReadHook", () -> new GhostChannelMarkAsReadHook().install(lpparam.classLoader)); // Channel Mark as Read Button

                    try {
                        if (!skip("GhostTypingIndicatorHook")) new GhostTypingIndicatorHook().handleTypingBlock(dexKitBridge); // DM Typing
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | GhostTyping): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("GhostScreenshotDetectionHook")) new GhostScreenshotDetectionHook().handleScreenshotBlock(dexKitBridge); // Screenshot
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | GhostScreenshot): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("ScreenshotPermissionHook")) new ScreenshotPermissionHook().install(lpparam.classLoader); // Allow Screenshots
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ScreenshotPermission): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("GhostViewOnceHook")) new GhostViewOnceHook().handleViewOnceBlock(dexKitBridge); // View Once
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | GhostViewOnce): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("GhostStorySeenHook")) new GhostStorySeenHook().handleStorySeenBlock(dexKitBridge); // Story Seen
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | GhostStorySeen): ❌ Failed to hook");
                    }

                    try {
                        io.github.somilkhan.instaeclipse2.utils.ghost.CurrentConversation.install(dexKitBridge, lpparam.classLoader);
                        io.github.somilkhan.instaeclipse2.mods.ghost.ConversationMenuHook.install();
                        if (!skip("KeepUnsentMessagesHook")) new KeepUnsentMessagesHook().install(dexKitBridge, lpparam.classLoader); // Keep Unsent
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | KeepUnsent): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("UnsentThreadButtonHook")) new io.github.somilkhan.instaeclipse2.mods.ghost.UnsentThreadButtonHook().install(lpparam.classLoader); // per-thread unsent button
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | UnsentBtn): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("HideChatsHook")) new io.github.somilkhan.instaeclipse2.mods.ghost.HideChatsHook().install(dexKitBridge, lpparam.classLoader); // Hide Specific Chats
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | HideChats): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("CustomFontHook")) new io.github.somilkhan.instaeclipse2.mods.ui.CustomFontHook().install(lpparam.classLoader); // Custom UI font
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | CustomFont): ❌ Failed to hook");
                    }

                    io.github.somilkhan.instaeclipse2.mods.ui.RemoveMetaAIHook metaAi = new io.github.somilkhan.instaeclipse2.mods.ui.RemoveMetaAIHook();
                    isolatedInstall("metaAi", () -> metaAi.install(lpparam.classLoader));              // composer/search XML layouts
                    isolatedInstall("metaAi.reels", () -> metaAi.installReels(dexKitBridge, lpparam.classLoader)); // reels Litho unit (#179)

                    // Disable Repost (feed + reels) — UI/action level; network drop is ineffective
                    try {
                        if (!skip("DisableRepostHook")) new io.github.somilkhan.instaeclipse2.mods.ui.DisableRepostHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | DisableRepost): ❌ Failed to hook");
                    }

                    try {
                        if (!skip("LockDirectMessagesHook")) new io.github.somilkhan.instaeclipse2.mods.ui.LockDirectMessagesHook().install(lpparam.classLoader); // Lock DMs (#182)
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | LockDMs): ❌ Failed to hook");
                    }

                    // Hide in-feed widget units (suggested users panels, surveys, carousels, etc.)
                    try {
                        if (!skip("HideSuggestedFeedItemsHook")) new HideSuggestedFeedItemsHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | HideSuggested): ❌ Failed to hook");
                    }

                    // Ads Blocker
                    try {
                        if (!skip("AdBlocker")) new AdBlocker().disableSponsoredContent(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | AdBlocker): ❌ Failed to hook");
                    }

                    // tracking link disable
                    try {
                        if (!skip("TrackingLinkDisable")) new TrackingLinkDisable().disableTrackingLinks(hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | TrackingLinkDisable): ❌ Failed to hook");
                    }

                    // Exact timestamps instead of "3h ago" (#232)
                    try {
                        if (!skip("ExactTimestampHook")) new io.github.somilkhan.instaeclipse2.mods.misc.ExactTimestampHook().install(dexKitBridge);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ExactTime): ❌ Failed to hook");
                    }

                    // Auto-expand feed captions (#246)
                    try {
                        if (!skip("ExpandCaptionsHook")) new io.github.somilkhan.instaeclipse2.mods.feed.ExpandCaptionsHook().install(dexKitBridge);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ExpandCaptions): ❌ Failed to hook");
                    }

                    // Hide community notes (#259)
                    try {
                        if (!skip("HideCommunityNotesHook")) new io.github.somilkhan.instaeclipse2.mods.feed.HideCommunityNotesHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | CommunityNotes): ❌ Failed to hook");
                    }

                    // Extras: tapping a reel mutes instead of pausing
                    try {
                        if (!skip("ReelsControlsHook")) new io.github.somilkhan.instaeclipse2.mods.extras.ReelsControlsHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | Reels): ❌ Failed to hook: " + t);
                    }

                    // Extras: lift the 5-account limit
                    try {
                        if (!skip("AccountLimitHook")) new io.github.somilkhan.instaeclipse2.mods.extras.AccountLimitHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | UnlimitedAccounts): ❌ Failed to hook: " + t);
                    }

                    // Extras: keep Instagram's realtime connection down (appear offline)
                    try {
                        if (!skip("AirplaneModeHook")) new io.github.somilkhan.instaeclipse2.mods.extras.AirplaneModeHook().install(hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | Airplane): ❌ Failed to hook: " + t);
                    }

                    // Extras: pick the largest image candidate
                    try {
                        if (!skip("HighResolutionImageHook")) new io.github.somilkhan.instaeclipse2.mods.extras.HighResolutionImageHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | HighResImages): ❌ Failed to hook: " + t);
                    }

                    // Extras: skip contact/location onboarding pages
                    try {
                        if (!skip("OnboardingPromptHook")) new io.github.somilkhan.instaeclipse2.mods.extras.OnboardingPromptHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | Onboarding): ❌ Failed to hook: " + t);
                    }

                    // Extras: add FLAG_IMMUTABLE to IG's legacy push registration intent
                    try {
                        if (!skip("NotificationRegistrationHook")) new io.github.somilkhan.instaeclipse2.mods.extras.NotificationRegistrationHook().install();
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | NotificationFix): ❌ Failed to hook: " + t);
                    }

                    // Extras: no swipe to camera from the feed
                    try {
                        if (!skip("SwipeToCameraHook")) new io.github.somilkhan.instaeclipse2.mods.extras.SwipeToCameraHook().install(hostClassLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | SwipeCamera): ❌ Failed to hook: " + t);
                    }

                    // home feed shows only accounts you follow
                    try {
                        if (!skip("FollowingOnlyFeedHook")) new io.github.somilkhan.instaeclipse2.mods.feed.FollowingOnlyFeedHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | FollowingOnlyFeed): ❌ Failed to hook");
                    }

                    // "Follows you" label on profiles (#239)
                    try {
                        if (!skip("ProfileFollowsYouHook")) new io.github.somilkhan.instaeclipse2.mods.ui.ProfileFollowsYouHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | FollowsYou): ❌ Failed to hook");
                    }

                    // Playback speed for feed videos + forced reel seek bar (#247)
                    try {
                        if (!skip("PlaybackSpeedHook")) new io.github.somilkhan.instaeclipse2.mods.media.PlaybackSpeedHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | PlaybackSpeed): ❌ Failed to hook");
                    }

                    // Hide the DM Notes tray (#241)
                    try {
                        if (!skip("HideNotesTrayHook")) new io.github.somilkhan.instaeclipse2.mods.ui.HideNotesTrayHook().install(dexKitBridge);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | NotesTray): ❌ Failed to hook");
                    }

                    // Ported from Piko: feed share/save/repost/like-count, profile highlights,
                    // hidden requests row, empty bottom space. Each install fails closed and logs.
                    isolatedInstall("FeedUfiButtonsHook", () -> new io.github.somilkhan.instaeclipse2.mods.feed.FeedUfiButtonsHook().install(dexKitBridge));
                    isolatedInstall("ProfileHighlightsHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.ProfileHighlightsHook().install(dexKitBridge));
                    isolatedInstall("HideHiddenRequestsHook", () -> new io.github.somilkhan.instaeclipse2.mods.ghost.HideHiddenRequestsHook().install());
                    isolatedInstall("BottomSpaceHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.BottomSpaceHook().install(dexKitBridge));

                    // Ported from Piko (batch 2). One try per hook so a failure can't take the
                    // others down; each install also logs and skips on its own when its target
                    // isn't found.
                    isolatedInstall("FollowButtonsHook.stories", () -> new io.github.somilkhan.instaeclipse2.mods.piko.FollowButtonsHook().installStories(dexKitBridge));
                    isolatedInstall("FollowButtonsHook.reels", () -> new io.github.somilkhan.instaeclipse2.mods.piko.FollowButtonsHook().installReels());
                    isolatedInstall("LoopStoryHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.LoopStoryHook().install(dexKitBridge));
                    isolatedInstall("DisappearingSwipeHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.DisappearingSwipeHook().install(dexKitBridge));
                    isolatedInstall("ChatCallButtonsHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.ChatCallButtonsHook().install(dexKitBridge));
                    isolatedInstall("StoryTrayFilterHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.StoryTrayFilterHook().install(dexKitBridge));
                    isolatedInstall("StoryRingSizeHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.StoryRingSizeHook().install(dexKitBridge));
                    isolatedInstall("ResumeReelsHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.ResumeReelsHook().install(dexKitBridge));
                    isolatedInstall("MarkReadAfterReplyHook", () -> new io.github.somilkhan.instaeclipse2.mods.piko.MarkReadAfterReplyHook().install(dexKitBridge));

                    // Download DM voice messages (#223)
                    try {
                        if (!skip("VoiceMessageDownloadHook")) new io.github.somilkhan.instaeclipse2.mods.media.VoiceMessageDownloadHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|VoiceDL) ❌ Failed to hook");
                    }

                    // Keep DM edit history (#243)
                    try {
                        if (!skip("EditHistoryHook")) new io.github.somilkhan.instaeclipse2.mods.ghost.EditHistoryHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|EditHistory) ❌ Failed to hook");
                    }

                    // Jump to the first message of a DM (#244)
                    isolatedInstall("JumpToFirstMessageHook", () -> new io.github.somilkhan.instaeclipse2.mods.ghost.JumpToFirstMessageHook().install());
                    isolatedInstall("ReadReceiptChatHook", () -> new io.github.somilkhan.instaeclipse2.mods.ghost.ReadReceiptChatHook().install(hostClassLoader));

                    // Search your story viewers (Plus-style)
                    try {
                        if (!skip("StoryViewerSearchHook")) new io.github.somilkhan.instaeclipse2.mods.ui.StoryViewerSearchHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|ViewerSearch) ❌ Failed to hook");
                    }

                    // Search inside the comments sheet (#230)
                    try {
                        if (!skip("CommentSearchHook")) new io.github.somilkhan.instaeclipse2.mods.misc.CommentSearchHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|CommentSearch) ❌ Failed to hook");
                    }

                    // Export a DM chat to HTML (#225)
                    try {
                        if (!skip("ChatExportHook")) new io.github.somilkhan.instaeclipse2.mods.ghost.ChatExportHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|ChatExport) ❌ Failed to hook");
                    }

                    // Export followers / following to CSV (#224)
                    try {
                        if (!skip("FollowListExportHook")) new io.github.somilkhan.instaeclipse2.mods.misc.FollowListExportHook().install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|FollowExport) ❌ Failed to hook");
                    }

                    // Like undo window (#235) — after HideSuggestedFeedItemsHook (has_liked getter)
                    try {
                        if (!skip("LikeUndoHook")) new io.github.somilkhan.instaeclipse2.mods.misc.LikeUndoHook().install(dexKitBridge);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | LikeUndo): ❌ Failed to hook");
                    }

                    // Hide "Create group" on the DM share sheet (#240)
                    try {
                        if (!skip("ShareSheetCleanupHook")) new io.github.somilkhan.instaeclipse2.mods.ui.ShareSheetCleanupHook().install(hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ShareSheet): ❌ Failed to hook");
                    }

                    // Open links in the external browser (#229)
                    try {
                        if (!skip("ExternalBrowserHook")) new io.github.somilkhan.instaeclipse2.mods.misc.ExternalBrowserHook().install();
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ExternalBrowser): ❌ Failed to hook");
                    }

                    // Miscellaneous
                    try {
                        if (!skip("DisableStoryFlippingHook")) new DisableStoryFlippingHook().handleStoryFlippingDisable(dexKitBridge); // Story Flipping
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | StoryFlipping): ❌ Failed to hook");
                    }

                    // Story Mentions
                    isolatedInstall("StoryMentionHook", () -> new StoryMentionHook().install(dexKitBridge, lpparam.classLoader));
                    isolatedInstall("StoryLinksHook", () -> new io.github.somilkhan.instaeclipse2.mods.misc.StoryLinksHook().install(dexKitBridge, lpparam.classLoader));
                    isolatedInstall("StoryInfoHook", () -> new io.github.somilkhan.instaeclipse2.mods.misc.StoryInfoHook().install(dexKitBridge, lpparam.classLoader));

                    // Comment Copy
                    try {
                        if (!skip("CommentCopyHook")) new CommentCopyHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | CopyComment): ❌ Failed to hook");
                    }

                    // Caption Copy
                    try {
                        if (!skip("CaptionCopyContextMenuHook")) new CaptionCopyContextMenuHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | Caption): ❌ Failed to hook");
                    }

                    // Disable Double Tap to Like
                    try {
                        if (!skip("DisableDoubleTapLikeHook")) new DisableDoubleTapLikeHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | DoubleTapLike): ❌ Failed to hook");
                    }
                    // ...and in comments and DMs
                    try {
                        if (!skip("DoubleTapExtrasHook")) new io.github.somilkhan.instaeclipse2.mods.misc.DoubleTapExtrasHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | DoubleTapLike): ❌ comments/DMs: " + t);
                    }

                    // Photo Zoom (long-press)
                    try {
                        if (!skip("FeedPhotoZoomHook")) new FeedPhotoZoomHook().install(lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | PhotoZoom): ❌ Failed to hook");
                    }

                    // Location Spoof
                    try {
                        if (!skip("LocationSpoofHook")) new LocationSpoofHook().install(lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | SpoofLocation): ❌ Failed to hook");
                    }

                    // Custom Theme
                    try {
                        if (!skip("IgThemeHook")) new IgThemeHook().install(hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | Theme): ❌ Failed to hook");
                    }

                    // Force Reel Quality
                    try {
                        if (!skip("ForceReelQualityHook")) new ForceReelQualityHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ Failed to hook");
                    }

                    // Force "Upload at highest quality" always on
                    try {
                        if (!skip("MaxUploadQualityHook")) new io.github.somilkhan.instaeclipse2.mods.media.MaxUploadQualityHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|MaxUploadQuality) ❌ Failed to hook");
                    }

                    try {
                        if (!skip("DisableVideoAutoPlayHook")) new DisableVideoAutoPlayHook().handleAutoPlayDisable(dexKitBridge); // Video Autoplay
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | AutoPlayDisable): ❌ Failed to hook");
                    }

                    // Build Expired Popup
                    try {
                        if (!skip("BuildExpiredPopupHook")) new BuildExpiredPopupHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | BuildExpired): ❌ Failed to hook");
                    }

                    // Media Download (feed)
                    isolatedInstall("FeedVideoDownloadHook", () -> new FeedVideoDownloadHook().install(lpparam.classLoader));
                    isolatedInstall("FeedVideoDownloadHook.videoUrlCapture", () -> FeedVideoDownloadHook.installVideoUrlCaptureHook(dexKitBridge, lpparam.classLoader));

                    // Post Download — three-dots menu (replaces floating button + long-press)
                    try {
                        if (!skip("PostDownloadContextMenuHook")) new PostDownloadContextMenuHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | PostDownload): ❌ Failed to hook");
                    }

                    // Save Instants (#184) — long-press a received Instant (quicksnap) to save it
                    try {
                        if (!skip("InstantSaveHook")) new io.github.somilkhan.instaeclipse2.mods.media.InstantSaveHook().install(lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | InstantSave): ❌ Failed to hook");
                    }

                    // Upload Instants from gallery (#199) — swap gallery bitmap into quicksnap send
                    try {
                        if (!skip("InstantUploadHook")) new io.github.somilkhan.instaeclipse2.mods.media.InstantUploadHook().install(lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | InstantUpload): ❌ Failed to hook");
                    }

                    // Keep Ephemeral Messages
                    try {
                        if (!skip("GhostEphemeralKeepHook")) new GhostEphemeralKeepHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | EphemeralHook): ❌ Failed to hook");
                    }

                    // Permanent View Mode (view-once / view-twice → permanent)
                    try {
                        if (!skip("GhostPermanentViewHook")) new GhostPermanentViewHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ViewOnceMedia): ❌ Failed to hook");
                    }

                    // Restore IG's native view-once/twice corner icon when Permanent View is on
                    try {
                        if (!skip("ViewOnceBadgeHook")) new ViewOnceBadgeHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | VOBadge): ❌ Failed to hook");
                    }

                    // Locked saved collections (#219)
                    try {
                        if (!skip("LockedCollectionsHook")) new io.github.somilkhan.instaeclipse2.mods.ui.LockedCollectionsHook().install(hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|LockedColl) ❌ Failed to hook");
                    }

                    // Saved button on your own profile (#237)
                    isolatedInstall("SavedOnProfileHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.SavedOnProfileHook().install(dexKitBridge, hostClassLoader));
                    isolatedInstall("HideHeaderButtonsHook", () -> new io.github.somilkhan.instaeclipse2.mods.ui.HideHeaderButtonsHook().install(hostClassLoader));

                    // Download a post/reel link shared to InstaEclipse from another app (#236)
                    try {
                        if (!skip("ShareLinkDownloadHook")) new io.github.somilkhan.instaeclipse2.mods.media.ShareLinkDownloadHook().install(hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(IE|ShareDL) ❌ Failed to hook");
                    }

                    // Story Download
                    try {
                        if (!skip("StoryDownloadHook")) new StoryDownloadHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | StoryDownload): ❌ Failed to hook");
                    }

                    // Reel Download
                    try {
                        if (!skip("ReelDownloadHook")) new ReelDownloadHook().install(dexKitBridge, lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ReelDownload): ❌ Failed to hook");
                    }

                    // Profile Picture Download
                    try {
                        if (!skip("ProfilePicDownloadHook")) ProfilePicDownloadHook.install(dexKitBridge, hostClassLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ProfileDownload): ❌ Failed to hook");
                    }

                    // Network Interceptor
                    try {
                        if (!skip("interceptor")) interceptor.handleInterceptor(lpparam);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | Interceptor): ❌ Failed to hook");
                    }

                    // Crash guard: drop tasks rejected by already-shut-down executors (carousel/
                    // realtime teardown race on IG 446+/447.0.0.39+) instead of letting AbortPolicy
                    // throw and hard-crash the app.
                    try {
                        if (!skip("TerminatedExecutorGuard")) new io.github.somilkhan.instaeclipse2.mods.core.TerminatedExecutorGuard().install(lpparam.classLoader);
                    } catch (Throwable ignored) {
                        ModuleLog.line("(InstaEclipse | ExecGuard): ❌ Failed to hook");
                    }

                    // Every lookup is cached now: later launches take the warm path.
                    DexKitCache.saveString(CACHE_COMPLETE_KEY, "1");
                    };

                    boolean warm = DexKitCache.isCacheValid() && "1".equals(DexKitCache.loadString(CACHE_COMPLETE_KEY));
                    if (!warm) {
                        // Say why, so a log that repeats this line every launch can be told apart:
                        // "cache reset" = new Instagram or module build; "previous run unfinished" =
                        // Instagram was closed or crashed before the background pass got to the end.
                        ModuleLog.line("(InstaEclipse): first launch for this Instagram version ("
                                + (DexKitCache.isCacheValid() ? "previous run unfinished" : "cache reset")
                                + ") — installing hooks in background");
                    }
                    Thread t = new Thread(() -> {
                        if (!INSTALL_COORDINATOR.tryStart()) {
                            ModuleLog.line("(InstaEclipse): duplicate hook sweep ignored; state="
                                    + INSTALL_COORDINATOR.getState());
                            return;
                        }
                        long start = android.os.SystemClock.elapsedRealtime();
                        try {
                            installFeatureHooks.run();
                            INSTALL_COORDINATOR.markComplete();
                        } catch (Throwable e) {
                            // A partial sweep is not a successful warm-cache run. Leave the
                            // completion key unset so the next process retries the cold path.
                            INSTALL_COORDINATOR.markFailed();
                            ModuleLog.line("(InstaEclipse): ❌ background install: " + e);
                        }
                        long took = android.os.SystemClock.elapsedRealtime() - start;
                        ModuleLog.line("(InstaEclipse): " + (warm ? "cached" : "cold")
                                + " hook sweep state=" + INSTALL_COORDINATOR.getState()
                                + " duration=" + took + " ms");
                    }, "InstaEclipse-Init");
                    // DexKit runs each query on every core by default, which starved Instagram's
                    // own startup ("failed to complete startup" ANR). Keep half the cores free.
                    try {
                        int cores = Runtime.getRuntime().availableProcessors();
                        dexKitBridge.setThreadNum(Math.max(1, cores / 2));
                    } catch (Throwable ignored) { }
                    if (warm) {
                        // Cached discovery still runs in the worker: per-process Xposed callbacks
                        // must be reinstalled, and the cache is not proof that hooks are live.
                        t.setPriority(Thread.NORM_PRIORITY - 1);
                    } else {
                        // First launch: full DexKit discovery can take many seconds. Start on the
                        // next main-loop turn after attach returns and keep all discovery off UI.
                        t.setPriority(Thread.MIN_PRIORITY);
                    }
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(t::start);
                }

            });

        } catch (Exception e) {
            ModuleLog.line("(InstaEclipse): Failed to hook " + lpparam.packageName + ": " + e.getMessage());
        }
    }

    /**
     * Injects a dynamic receiver into Instagram to listen for settings changes
     * sent from the InstaEclipse companion app (FeaturesFragment staging system).
     */
    private void registerSyncReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (action != null && action.startsWith("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF")
                        && !io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.isPreferenceUpdate(action,
                            intent.getStringExtra("key"), intent.getExtras() == null ? null : intent.getExtras().get("value"))) return;
                if ("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF".equals(action)) {
                    String key = intent.getStringExtra("key");
                    io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.markApplied(ctx, key, intent.getLongExtra("ts", 0));
                    boolean value = intent.getBooleanExtra("value", false);

                    ModuleLog.line("(InstaEclipse) Sync: Updating " + key + " to " + value);

                    android.content.SharedPreferences prefs = ctx.getSharedPreferences(io.github.somilkhan.instaeclipse2.utils.core.SettingsManager.PREF_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean(key, value).apply();

                    SettingsManager.loadAllFlags(ctx);
                    if ("isDevEnabled".equals(key)) DevOptionsUnlockHook.resetRuntimeLogForFlagChange();
                    if ("airplaneMode".equals(key)) io.github.somilkhan.instaeclipse2.mods.extras.AirplaneModeHook.apply();
                    FeatureManager.refreshFeatureStatus();
                    io.github.somilkhan.instaeclipse2.mods.ui.theme.ThemeRefresh.onPrefChanged(key);

                } else if ("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_STRING".equals(action)) {
                    String key = intent.getStringExtra("key");
                    io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.markApplied(ctx, key, intent.getLongExtra("ts", 0));
                    String value = intent.getStringExtra("value");

                    ModuleLog.line("(InstaEclipse) Sync: Updating string pref " + key);

                    android.content.SharedPreferences prefs = ctx.getSharedPreferences(io.github.somilkhan.instaeclipse2.utils.core.SettingsManager.PREF_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putString(key, value).apply();

                    SettingsManager.loadAllFlags(ctx);
                    io.github.somilkhan.instaeclipse2.mods.ui.theme.ThemeRefresh.onPrefChanged(key);

                } else if ("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_INT".equals(action)) {
                    String key = intent.getStringExtra("key");
                    io.github.somilkhan.instaeclipse2.utils.core.CompanionSync.markApplied(ctx, key, intent.getLongExtra("ts", 0));
                    int value = intent.getIntExtra("value", 0);

                    ModuleLog.line("(InstaEclipse) Sync: Updating int pref " + key + " to " + value);

                    android.content.SharedPreferences prefs = ctx.getSharedPreferences(io.github.somilkhan.instaeclipse2.utils.core.SettingsManager.PREF_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putInt(key, value).apply();

                    SettingsManager.loadAllFlags(ctx);
                    FeatureManager.refreshFeatureStatus();
                    io.github.somilkhan.instaeclipse2.mods.ui.theme.ThemeRefresh.onPrefChanged(key);

                } else if (CommonUtils.ACTION_REQUEST_LOGS.equals(action)) {
                    // Re-attach/rewrite the companion-side mirror file first (covers the companion
                    // having cleared its data), then reply; the companion re-reads the file on
                    // reply and only uses the inline text as a fallback.
                    Logging.refreshMirror(ctx, () -> {
                        try {
                            Intent reply = new Intent(CommonUtils.ACTION_LOGS_REPLY);
                            reply.setPackage(CommonUtils.MY_PACKAGE_NAME);
                            // Mirror active -> companion reads the file; skip the big (binder-limited) extra.
                            reply.putExtra(CommonUtils.EXTRA_LOG_TEXT, Logging.isMirrorActive() ? "" : Logging.getSnapshotForIpc());
                            reply.putExtra(CommonUtils.EXTRA_LOG_SOURCE, ctx.getPackageName());
                            io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());
                        } catch (Throwable t) {
                            Intent reply = new Intent(CommonUtils.ACTION_LOGS_REPLY);
                            reply.setPackage(CommonUtils.MY_PACKAGE_NAME);
                            reply.putExtra(CommonUtils.EXTRA_LOG_ERROR, "Logs are unavailable");
                            reply.putExtra(CommonUtils.EXTRA_LOG_SOURCE, ctx.getPackageName());
                            io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());
                        }
                    });

                } else if (CommonUtils.ACTION_CLEAR_LOGS.equals(action)) {
                    Logging.clear();

                } else if ("io.github.somilkhan.instaeclipse2.ACTION_REQUEST_PREFS".equals(action)) {
                    ModuleLog.line("(InstaEclipse) Sync: Companion app requested current preferences.");

                    android.content.SharedPreferences prefs = ctx.getSharedPreferences(io.github.somilkhan.instaeclipse2.utils.core.SettingsManager.PREF_NAME, Context.MODE_PRIVATE);
                    Intent reply = new Intent("io.github.somilkhan.instaeclipse2.ACTION_SEND_PREFS");
                    reply.setPackage("io.github.somilkhan.instaeclipse2");

                    Bundle bundle = new Bundle();
                    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                        // The lock passcode hash and its salt never leave Instagram's process.
                        if ("lockDirectPasscode".equals(entry.getKey()) || "lockDirectSalt".equals(entry.getKey())) continue;
                        // Change stamps, so the companion keeps its own newer changes (PrefsMerge).
                        if (io.github.somilkhan.instaeclipse2.utils.core.PrefsMerge.isSharedStamp(entry.getKey(), entry.getValue())) {
                            bundle.putLong(entry.getKey(), (Long) entry.getValue());
                        } else if (entry.getValue() instanceof Boolean) {
                            bundle.putBoolean(entry.getKey(), (Boolean) entry.getValue());
                        } else if (entry.getValue() instanceof String) {
                            bundle.putString(entry.getKey(), (String) entry.getValue());
                        } else if (entry.getValue() instanceof Integer) {
                            bundle.putInt(entry.getKey(), (Integer) entry.getValue());
                        }
                    }
                    reply.putExtras(bundle);
                    io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());

                } else if ("io.github.somilkhan.instaeclipse2.ACTION_EXPORT_CONFIG".equals(action)) {
                    ModuleLog.line("(InstaEclipse) Sync: Companion app requested Dev Config export.");
                    try {
                        java.io.File source = new java.io.File(ctx.getFilesDir(), "mobileconfig/mc_overrides.json");
                        if (!source.exists()) {
                            ModuleLog.line("(InstaEclipse) Export: mc_overrides.json not found.");
                            Intent reply = new Intent("io.github.somilkhan.instaeclipse2.ACTION_SEND_CONFIG");
                            reply.setPackage("io.github.somilkhan.instaeclipse2");
                            reply.putExtra("error", "mc_overrides.json not found.");
                            io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());
                            return;
                        }
                        String json;
                        try (java.io.InputStream input = new java.io.FileInputStream(source)) {
                            json = io.github.somilkhan.instaeclipse2.utils.ipc.BrokerPayload.readConfig(input);
                        }
                        Intent reply = new Intent("io.github.somilkhan.instaeclipse2.ACTION_SEND_CONFIG");
                        reply.setPackage("io.github.somilkhan.instaeclipse2");
                        reply.putExtra("json_content", json);
                        io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());
                        ModuleLog.line("(InstaEclipse) Export: config reply sent to companion.");
                    } catch (Exception e) {
                        Intent reply = new Intent("io.github.somilkhan.instaeclipse2.ACTION_SEND_CONFIG");
                        reply.putExtra("error", "Configuration could not be read or exceeds the supported size");
                        io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.echoNonce(intent, reply);
                        io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.reply(intent, reply.getExtras());
                        ModuleLog.line("(IE|Config) Configuration export failed");
                    }

                } else if ("io.github.somilkhan.instaeclipse2.ACTION_BACKUP_SETTINGS".equals(action)) {
                    ModuleLog.line("(InstaEclipse) Sync: Companion app requested Settings backup.");
                    try {
                        String json = io.github.somilkhan.instaeclipse2.utils.backup.SettingsBackupManager.toJson();
                        Intent exportIntent = new Intent();
                        exportIntent.setComponent(new android.content.ComponentName("io.github.somilkhan.instaeclipse2", "io.github.somilkhan.instaeclipse2.mods.devops.config.JsonExportActivity"));
                        exportIntent.putExtra("json_content", json);
                        exportIntent.putExtra("file_name", "instaeclipse_settings.json");
                        exportIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        io.github.somilkhan.instaeclipse2.utils.ipc.CompanionBridge.launchActivity(io.github.somilkhan.instaeclipse2.mods.ui.UIHookManager.getCurrentActivity(), exportIntent);
                    } catch (Exception e) {
                        ModuleLog.line("(InstaEclipse) Failed to create backup: " + e.getMessage());
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF");
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_STRING");
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_UPDATE_PREF_INT");
        filter.addAction(CommonUtils.ACTION_REQUEST_LOGS);
        filter.addAction(CommonUtils.ACTION_CLEAR_LOGS);
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_REQUEST_PREFS");
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_EXPORT_CONFIG");
        filter.addAction("io.github.somilkhan.instaeclipse2.ACTION_BACKUP_SETTINGS");

        // Only the companion (holding the signature permission) can reach this receiver.
        io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.registerCompanionOnlyReceiver(context, receiver, filter);
    }
}
