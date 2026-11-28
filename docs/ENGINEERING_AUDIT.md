# Engineering audit

Recorded 2026-11-28. Branch `maintainer/eng-audit` (PR #22, base `main` at `17f5bd0`). Release under audit: 0.9.0
(versionCode 23). versionName/versionCode were **not** changed.

## Scope and method

- **Repo intelligence**: module entry `Xposed/Module.java`, which runs 93 isolated hook installs over 12 `mods/` packages (ads, core,
  devops, extras, feed, ghost, location, media, misc, network, piko, ui). It has 171 `FeatureFlags` and about 151 companion switches.
- **Hook verification**: `tools/hook-verify` (kept as is; only its enum decoder and install parser were fixed) ran every
  install against exact APKs with DexKit 2.0.7:
  - 457.0.0.55.73 (386314291, arm64, SHA-256 `956023e1…60152d6`)
  - 458.0.0.0.57 (386404709, arm64, SHA-256 `2573880a…2be763`)
- **Static review** by area (hooks, Instagram compatibility, QA, platform, media, security). Findings are in
  [BUG_TRACKER.md](BUG_TRACKER.md).
- **Not done**: device testing. No build has been run on a phone with this branch.

## Agent activity (actual)

- **Round 1 (earlier session)**: specialist agents produced 10 fix branches and PRs #12–#21, all with green CI. Their written
  reports did not survive the session. Only the commits (and the IDs in them) remain.
- **Round 2 (this session)**: no sub-agents were spawned. The CTO agent did the integration, toolchain, APK acquisition,
  hook-verify runs and the fixes ENG-07..ENG-12 directly.

## Hook verification results

| Build | Steps | OK | PARTIAL | FAIL | NO-HOOK | Report |
|---|---|---|---|---|---|---|
| 457.0.0.55.73 (at 562bd0b, before ENG-09) | 93 | 85 | 1 | 2 | 5 | (superseded) |
| 457.0.0.55.73 (now) | 93 | 86 | 1 | 1 | 5 | [report](hook-verify/report-457.0.0.55.73-386314291.md) |
| 458.0.0.0.57 (before ENG-10/11) | 93 | 84 | 3 | 1 | 5 | (superseded) |
| 458.0.0.0.57 (now) | 93 | 86 | 1 | 1 | 5 | [report](hook-verify/report-458.0.0.0.57-386404709.md) |

The remaining non-OK steps are identical on both builds:
- VoiceMessageDownload PARTIAL and LocationSpoof FAIL come from the harness's stubs.
- NotificationRegistration, JumpToFirstMessage, ReadReceiptChat, ExternalBrowser and FeedVideoDownload#install are NO-HOOK because they only install at runtime.

Offline OK means the lookups resolved. It does **not** mean the feature works on a phone.

## Key findings

1. **Reliability**: one throw in Module.java used to skip every later hook (ENG-01). DexKit 2.0.3 was dead on 16 KB-page
   kernels (ENG-02). Both are fixed.
2. **Security**: share-download intents were unauthenticated (SEC-02), providers trusted package names (SEC-04), the
   clone trust store was backed up (SEC-07), and logs leaked signed URLs (ENG-03). All are fixed.
3. **Compatibility on 458**: the reel scrubber gate was inlined (ENG-10). The fix is now checked offline. Because the
   isVideo log ignored the hashed path, the downloader looked broken when it wasn't (ENG-11).
4. **Data**: backup/restore lost settings and was not atomic (QA-01..08). The legacy download path skipped verification (MEDIA-01..04).
5. **Honesty of status**: the app called 457 "Tested" without a device test (ENG-05). It now shows "Checked offline".

## Features not yet audited in depth

These areas have offline lookup coverage but no behavioural review yet:
- Ghost-mode interference between switches (seen / typing / screenshot / view-once combinations).
- Story and reel playback interactions (loop, resume, speed, scrubber with ads).
- Companion UI flows other than backup and folder pickers.
- Issue #8 (Piko features that cannot be hooked on 458).

## Policy notes

- No device-fingerprint spoofing, anti-ban evasion or bulk automation was added.
- No verification criterion was weakened. The two harness changes make the decoder see more of the APK; they do not relax a check.
