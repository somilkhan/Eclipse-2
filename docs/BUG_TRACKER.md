# Bug tracker (engineering audit)

Branch `maintainer/eng-audit` (PR #22). Last updated 2026-11-28.

**Verification levels**: *unit* = JVM unit test in `app/src/test`; *offline* = `tools/hook-verify` against an exact
Instagram APK (457.0.0.55.73 / 458.0.0.0.57); *device* = run on a phone. **No fix below is device-verified.**

IDs `HOOK-`, `QA-`, `SEC-`, `PLAT-`, `MEDIA-` come from the first audit round (specialist agents). Fixes made without an
ID got an `ENG-` ID here. Priority: P0 crash/data loss/security, P1 feature broken, P2 feature degraded, P3 diagnostics,
P4 cosmetic.

## Fixed on the branch

| ID | P | Area / file | Root cause | Fix (commit) | Regression test | Verification |
|---|---|---|---|---|---|---|
| SEC-02 / PLAT-03 | P0 | Share downloads (`ShareProof`) | Any app could send Instagram a VIEW intent with the SHARE_DOWNLOAD extra and make it fetch and save under the user's session | Companion attaches an immutable PendingIntent; hook checks the creator (943df73) | `ShareProofTest` | unit, build |
| SEC-04 / PLAT-04 | P0 | `LogMirrorProvider`, `CrashReportProvider` | Callers trusted by package name only | Package + official/pinned signer, per-caller crash rate limit (943df73) | **none dedicated** | build only |
| SEC-01 | P1 | Signer pinning | Re-signed (LSPatch) Instagram could not be trusted | Allow pinning a re-signed `com.instagram.android` (943df73) | `CloneTrustHostTest` | unit |
| SEC-07 / PLAT-08 | P0 | `backup_rules.xml`, `data_extraction_rules.xml` | Clone trust store and crash bookkeeping went into Auto Backup/device transfer | Excluded in both rule files (90231db) | `BackupRulesTest` | unit |
| QA-01, QA-02, QA-04, QA-08, SEC-08 | P1 | `SettingsBackupManager`, `SettingsRestore` | Backup missed ~29 settings, restore aborted half-applied, companion backup dropped non-boolean values, Extreme Mode could be released by a backup | Reflection-derived backup, staged atomic restore, typed values, Extreme Mode respected (e5ac067) | `SettingsBackupManagerTest`, `SettingsRestoreTest` | unit |
| QA-03 | P1 | Post/reel menu click gate | Click gate ignored `externalDownloader`, so the row did nothing | Shared `MediaMenuFeatures` predicates (8a4f467) | `MediaMenuFeaturesTest` | unit |
| QA-05, QA-06 | P3 | Feature-load toast | Hide stories tray never marked hooked; AdBlocker shown as raw key | Mark hooked, register label (3261fed) | **none** | build only |
| HOOK-01 | P2 | `ThemeRefresh` | Every synced key recreated the foreground activity (N recreates per save) | Only theme keys, 300 ms debounce (c584fb4) | `ThemeRefreshTest` | unit |
| HOOK-02 | P3 | `RestartRequired` | Startup-only features looked broken when toggled | "Restart Instagram to apply" note (d5e8bf9) | `RestartRequiredTest` | unit |
| HOOK-03 | P1 | `PermanentViewModel` (ghost) | seen_count taken from the last int field incl. statics; partial rewrite on early return | Per-class resolution, decide before mutating (b8cda2b) | `PermanentViewModelTest` | unit |
| HOOK-07 | P1 | `LikeAnimationHook` | Only `LikeActionView` hooked; names matched by edit distance; mapper result unchecked | Activation metadata + alias table + restore when off (ca2c24d, 08cfc91, 311e037) | `PikoHelpersTest`, `LikeMetadataTrackerTest` | unit, offline OK (457, 458) |
| PLAT-05 | P0 | SAF folder pickers (`SafGrants`) | `takePersistableUriPermission` uncaught (crash); replaced grants never released | Catch + toast, release unless still used (e809179) | `SafGrantsTest` | unit |
| MEDIA-01..04 | P1 | Legacy download path | No HTTP status/size checks, MediaStore rows without IS_PENDING, overwrite, MediaMuxer cannot write VP9 | Routed through the verified fetch, join and publisher (2e9d5f1) | `LegacyMediaSaveTest` | unit |
| MEDIA-06, MEDIA-07 | P2 | Original fallback, highlight "download all" | Fallback only logged; tally counted attempts as saves | User-visible fallback note, `HighlightTally` (702c703) | `HighlightTallyTest` | unit |
| ENG-01 | P1 | `Xposed/Module.java` | Seven shared try blocks: one throw skipped every later hook | `isolatedInstall` per hook; hook-verify parses it (f160cea) | `ModuleInstallIsolationTest` | unit, offline |
| ENG-02 | P0 | DexKit native lib | DexKit 2.0.3 not 16 KB-page aligned: module silently dead on 16 KB kernels | DexKit 2.0.7, catch `UnsatisfiedLinkError`, CI alignment check (b65f5e3) | `check-native-alignment.sh` in CI | build; **device: not tested on a 16 KB device** |
| ENG-03 | P1 | Module logs (`LogRedact`) | Signed CDN URLs, share links, usernames in exported logs | Redaction (8178217) | `LogRedactTest` | unit |
| ENG-04 | P2 | Log mirror | Size budget counted but not enforced | Compaction (d42e790) | `LoggingRedactionTest` | unit |
| ENG-05 | P2 | `IgCompat`, Home row | 458 alpha users told to install 457; "Tested" overclaimed | DEVICE_TESTED vs CHECKED_OFFLINE (6c51419) | `IgCompatTest` | unit |
| ENG-06 | P3 | `ReleaseVersionTest` | Failed whenever `## Unreleased` existed | Skip Unreleased (73b88be) | itself | unit |
| ENG-07 | P1 | `FollowButtonsHook` (stories) | bind on base X.0HCo, name() override on subclass X.058K; hook needed both in one class | `baseNameGetter` (e62208a) | `FollowButtonsHookTest` | offline FAIL→OK (457) |
| ENG-08 | P3 | hook-verify `EnumSupport` | Constants only in `filled-new-array` not decoded: false PARTIAL | Decoder fix (cd39691) | harness | offline PARTIAL→OK (457) |
| ENG-09 | P1 | `StoryTrayFilterHook` | One enum `toString()` throwing aborted the install | `hasConstant` isolates throws (ec51514) | `StoryTrayFilterHookTest` | offline FAIL→OK (457) |
| ENG-10 | P1 | `PlaybackSpeedHook` reel scrubber | 458: gate wrapper inlined, nothing to hook | Force config show-flags in the config factory (d30b11d) | `ReelScrubberFlagsTest` | offline PARTIAL→OK (458), 457 unchanged |
| ENG-11 | P3 | `FeedVideoDownloadHook` isVideo log | Log ignored the hashed `video_versions` path | Log names the real path (39c50cc) | n/a (log only) | offline PARTIAL→OK (458) |
| ENG-12 | P3 | Compat docs | 24d962c guessed a string-table move for the scrubber; wrong | Matrix corrected (c6dfb7a) | n/a | 458 bytecode |
| ENG-13 | P1 | `GhostDMMarkAsReadHook`, `GhostChannelMarkAsReadHook` (ghost-mode interference) | Mark as read set the user's `isGhostSeen` to false and then forced it to true: a button left on screen after Ghost seen was quick-toggled off turned it back on, a toggle during the 300–400 ms window was lost, and a callback that never ran left seen-blocking off. During the window, seen also goes through for every chat, not just the open one (still true) | New `GhostSeenBypass` window (expires after 2 s), checked by `GhostDMSeenHook` and the `/threads/…/opened` network drop; the setting is never written (this commit) | `GhostSeenBypassTest` | unit, offline (ghost steps OK on 457/458) |

## Open

| ID | P | Area | Status |
|---|---|---|---|
| OPEN-01 | P1 | Every fix above | **Device verification not done.** Needs a phone with 457.0.0.55.73 or 458.0.0.0.x. |
| OPEN-02 | P2 | 458.0.0.0.67 (release target) | Report is the old 79-step run; APK not obtainable from the sandbox. Re-run needed. |
| OPEN-03 | P2 | 458.0.0.0.72 (newest alpha) | No APK, no report. |
| OPEN-04 | P3 | hook-verify harness | VoiceMessageDownload PARTIAL and LocationSpoof FAIL are harness stubs (`Location.CREATOR`), not lookups. Harness could be improved. |
| OPEN-05 | P2 | Runtime-only hooks | NotificationRegistration, JumpToFirstMessage, ReadReceiptChat, ExternalBrowser and FeedVideoDownload#install can only be checked on a device. |
| OPEN-06 | P4 | `lintDebug` | Fails on MissingTranslation (mostly pre-existing); not a CI gate. |
| OPEN-07 | P2 | Issue #8 | Piko features that cannot be hooked on 458. Not yet triaged in this audit. |

## IDs not recovered

The first-round agents' reports were lost with that session. These IDs are referenced in the numbering but their findings
were never committed, so their content is unknown and they are **not** claimed as fixed: HOOK-04, HOOK-05, HOOK-06,
HOOK-08+, QA-07, SEC-03, SEC-05, SEC-06, PLAT-01, PLAT-02, PLAT-06, PLAT-07, MEDIA-05. They need a fresh audit.

## App and root (APP-xx)

Companion-app audit and opt-in root support (branch `maintainer/root-access`). Verification is unit tests and
`assembleDebug` only; **nothing here is device-tested**.

| ID | P | File | Root cause | Fix | Regression test | Status |
|---|---|---|---|---|---|---|
| APP-01 | P2 | `fragments/HomeFragment.java` `restartInstagram()` | Ran `su -c am force-stop` for every user with no opt-in, ignored failures, then relaunched after a fixed `postDelayed(400)`. | Uses `RootUi.restartInstagram`: root only when "Use root" is on and granted (checked off the main thread, logged), otherwise App info. No delay: launch runs after the root command returns. | `RootSessionTest.offByDefaultNeverStartsSu`, `RootCommandsTest` (force-stop quoting) | Unit tests + build |
| APP-02 | P2 | `fragments/FeaturesFragment.java` Tools → Restart App | Subtitle said "Force stop and reopen Instagram" but it only showed a toast. | Same `RootUi.restartInstagram` path, prefers the official package. | None (UI); shares APP-01 tests | Build only |
| APP-03 | P3 | `FeaturesFragment.resetCrashFolder()` / `resetDownloadFolder()` | Reset cleared the pref but never released the persisted SAF grant (apps have a limited number). | `SafGrants.releaseReplaced(old, null, otherSettingUri)`. | `SafGrantsTest.resetWithoutNewFolderReleasesUnlessShared` | Unit tests + build |
| APP-04 | P3 | `utils/crash/CrashReportProvider.java` | Write failure left an empty `.txt` in the user folder; all failures were swallowed silently; `capText` could split a surrogate pair. | Delete the empty document, log every failure (`Log.w` + module log), surrogate-safe cap. | None (needs a document provider) | Build only |
| APP-05 | P3 | `fragments/LoggingFragment.java` `clearLogs()` | Clear broadcast went to every installed package with a supported name, trusted or not. | `CommonUtils.broadcastToInstagram` (trusted hosts only). | None | Build only |
| APP-06 | P4 | `LoggingFragment.loadLogs()` | 3 s spinner even when no trusted Instagram was asked. | Skip the wait when no request was sent. | None | Build only |
| APP-07 | P4 | `fragments/HomeFragment.java` `updateFeatureCount()` / `refreshShortcuts()` | Count read unset keys as off, ignoring `FeatureFlags.defaultOn` (`lockUseFingerprint`); shortcut lock used `isExtremeMode` only, so an expired Focus Lock kept them locked. | `getBoolean(key, defaultOn(key))`; `SettingsRestore.isLocked(extreme, focusLockUntil, now)`. | Existing `SettingsRestore` lock tests cover the rule; no Home UI test | Build only |
| ROOT-01 | — | `utils/root/**`, Tools → Root access, Logs | Feature: opt-in root (`su` via `ProcessBuilder`, script on stdin, timeouts, capped output, status cache, single background thread). Actions: force-stop, clear DexKit cache, collect pending crashes, read module log. Each has the old non-root fallback and the UI says which path ran. Commands are built only in `RootCommands` from whitelisted packages and single-quoted paths. Pref excluded from backups. | — | `RootCommandsTest`, `RootSessionTest`, `RootActionsCrashTest`, `BackupRulesTest.rootOptInIsExcludedEverywhere` | Unit tests + build; not device-tested |

Not implemented with root (reasons): saving downloads to folders SAF cannot grant (download pipeline lives in `mods/`,
lead's area); checking LSPosed scope (needs LSPosed's private database). Nothing was added for fingerprint spoofing,
anti-ban, or hiding root/LSPosed.

