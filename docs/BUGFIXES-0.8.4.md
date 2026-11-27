# Bug fixes for 0.8.4 (branch `wip/bugs`)

Audit of the companion app and module infrastructure. Each entry lists file, symptom, cause, fix
and test. "Manual" means it needs a device (no emulator was available); the code path was checked
by reading it and by `assembleDebug` + lint.

## Crashes

| # | File | Symptom | Cause | Fix | Test |
|---|------|---------|-------|-----|------|
| 1 | `mods/media/DownloadSaveService.java` | Companion process crash when a download starts while the app is in the background (Android 12+, more often on 14/15). | `startForeground()` throws `ForegroundServiceStartNotAllowedException` and nothing caught it. | `enterForeground()` catches it, shows `ie_download_fgs_blocked`, stops the service cleanly. | Manual |
| 2 | `mods/media/DownloadSaveService.java` | On Android 15+ a dataSync service running over the 6 h daily limit gets the process killed with an ANR-style crash. | No `onTimeout(int, int)`. | Added `onTimeout`: cancels jobs, removes the notification, `stopSelf()`. (No `@Override` because compileSdk is 34.) | Manual |
| 3 | `mods/media/DownloadSaveService.java` | After a backup restore or a revoked folder grant every download failed with a generic error. | Saved tree URI used without checking the grant still exists. | `hasPersistedWriteGrant()` checks `getPersistedUriPermissions()`; falls back to the "no download folder" message so the user can pick again. | Manual |
| 4 | `fragments/HomeFragment.java` | Crash tapping "Download APK", "App info" or "Launch Instagram" on devices with no browser / settings activity blocked by policy. | `startActivity` without catching `ActivityNotFoundException`/`SecurityException`. | `safeStart(Intent)` + toast `ig_toast_no_app_to_open` (all locales). | Manual |
| 5 | `mods/devops/config/JsonExportActivity.java` | NPE when the provider returns no stream for the chosen file. | `assert out != null` is a no-op on Android. | Throw `IOException` (handled, shows error). Open with `"wt"` so a shorter export does not leave trailing bytes of an older file; falls back to default mode for providers without `"wt"`. | Manual |
| 6 | `mods/devops/config/JsonImportActivity.java` | NPE when the picker returned a null URI. | No null check. | Treated as cancelled. | Manual |
| 7 | `utils/core/CommonUtils.java` (`getVariantLabel`) | `StringIndexOutOfBoundsException` for package names like `a.b..c`; wrong labels on Turkish locale (`ınstander`). | `charAt(0)` on empty segment; `toLowerCase()` with default locale. | Guard empty segment, use `Locale.ROOT`. | `CommonUtilsVariantLabelTest` (3 tests) |

## Lifecycle / threading

| # | File | Symptom | Cause | Fix | Test |
|---|------|---------|-------|-----|------|
| 8 | `MainActivity.java` | After rotation / dark-mode switch / process death the bottom nav showed Home while another tab's fragment was restored; tapping the current tab again rebuilt it and lost its scroll/state. | `setSelectedItemId(nav_home)` ran on every `onCreate`; no reselect listener. | Select Home only when `savedInstanceState == null`; no-op `setOnItemReselectedListener`. | Manual |
| 9 | `MainActivity.java`, `utils/version/VersionCheckUtility.java` | Update check re-ran on every rotation (stacked dialogs); offline start showed a modal error every time. | Check called on every `onCreate`; failure path opened an error dialog. | Check only on fresh launch; failure is silent (logged). | Manual |
| 10 | `JsonExportActivity.java`, `JsonImportActivity.java` | Rotating during the file picker opened a second picker. | Picker launched in every `onCreate`. | Launch only when `savedInstanceState == null`. | Manual |
| 11 | `mods/location/LocationPickerActivity.java` | Possible crash / leaked activity when a search or reverse-geocode result arrived after the picker was closed; a huge response could OOM. | Worker posts to main thread without a liveness check (HTTP ignores `shutdownNow` interrupts); unbounded read. | `alive()` guard in all posted callbacks; response capped at 512 K chars. | Manual |
| 12 | `fragments/HomeFragment.java` (restart Instagram) | File-descriptor leak per root restart; `su` could be left running. | `Process` pipes never closed, `destroy()` only on timeout. | Close stdin first, close all pipes and `destroy()` in `finally`. | Manual |

## Settings sync / backup-restore

| # | File | Symptom | Cause | Fix | Test |
|---|------|---------|-------|-----|------|
| 13 | `fragments/FeaturesFragment.java`, new `utils/backup/SettingsRestore.java` | Restoring a settings backup only restored on/off switches (ints like intervals and strings like formats were lost), wrote unknown keys, could restore the DM lock passcode hash/salt and device-specific folder URIs/paths from another device, and read the whole file on the main thread without a size limit. | Ad-hoc parsing loop that only handled booleans. | `BrokerPayload.readConfig` (bounded) + `SettingsRestore.parse` (Gson): keeps only known preference types via `ModuleReplies.isPreferenceValue`, never restores `lockDirectPasscode`, `lockDirectSalt`, `downloaderCustomUri`, `downloaderCustomPath`, `customFontPath`, `customEmojiPath`; sends the right update action per type. | `SettingsRestoreTest` (7 tests) |
| 14 | `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Logs (may contain usernames/URLs) went to Google cloud backup and device transfer. | No excludes. | Exclude `ig_logs/`, `instaeclipse_companion.log`, `instaeclipse_logging.log`; settings are still backed up. | Manual (lint validates the XML) |

## i18n

| # | File | Symptom | Cause | Fix | Test |
|---|------|---------|-------|-----|------|
| 15 | `res/values-se/` → `res/values-sv/` | Swedish users saw English. | Swedish strings were in `values-se` (`se` = Northern Sami). The rename landed in commit `c77fb42` together with fix 8. | `git mv` to `values-sv`. | Lint/aapt build |
| 16 | `utils/core/CacheAutoClear.java`, `mods/ui/CustomFontHook.java`, `mods/ui/UIHookManager.java`, `mods/ghost/GhostChannelMarkAsReadHook.java`, `mods/ghost/KeepUnsentMessagesHook.java`, `mods/media/CommentMedia.java`, `mods/media/VoiceMessageDownloadHook.java`, `mods/ui/RemoveMetaAIHook.java`, `mods/misc/CaptionCopyContextMenuHook.java` | On a Turkish device: `Icons.ttf` was not recognised as an icon font, the `Cache` folder was skipped by auto-clear, "Search"/"Giphy" matches failed. | `toLowerCase()`/`toUpperCase()` with the default locale (`I` → `ı`). Lint `DefaultLocale`. | `Locale.ROOT` (one-token edits, no hook logic changed). | Covered by the same Locale.ROOT pattern in `CommonUtilsVariantLabelTest` |

## Lint

| # | File | Symptom | Cause | Fix | Test |
|---|------|---------|-------|-----|------|
| 17 | `mods/media/VoiceMessageDownloadHook.java` | `ie-gradle lintDebug` failed with 1 error (`SuspiciousIndentation`). | Lint false positive on a one-line `if (...) return;`. | Braces around the `return` (no behaviour change). | `lintDebug` |

## Noted, not changed

- `LoggingFragment.exportText()` builds the export with `sb.insert(0, …)` (quadratic, capped by `MAX_EXPORT_CHARS`). Left for `/root/features_dev`, which owns log export.
- `LogMirrorProvider` trusts clone packages by package name (no signature check) and does not cap the mirrored file size. Low risk (log text only).
- `AtomicFile` `.bak`/`.new` siblings of log files are not separately excluded from backup (the directory exclude covers `ig_logs/`).
- Lint `StaticFieldLeak` warnings are mostly in `mods/**` (Instagram process, activity refs held in statics); not changed to avoid overlap with the hook agents.

## Reviewed, no bug found

`DownloadBrokerService`, `CompanionBridge`, `ModuleReplies`, `IpcSecurity`, `PrefsSyncProvider`,
`CompanionSync`, `Logging`, `LoggingFragment` lifecycle, `VideoFramePickerActivity`, `Module`
sync receiver registration.
