# Instagram compatibility matrix

Status of Eclipse 2 **0.9.0 (versionCode 23)** against specific Instagram builds. Every row is backed by a
committed report or says that it isn't. Last updated 2026-11-28.

## Terms

- **Device-tested**: a maintainer ran this release on a phone with that exact build and used the features.
  This is `IgCompat.DEVICE_TESTED` in the code. **No build is device-tested for 0.9.0.**
- **Checked offline**: [`tools/hook-verify`](../tools/hook-verify/README.md) ran every hook install from
  `Xposed/Module.java` against that exact APK with DexKit. A lookup that resolves offline can still misbehave at
  runtime, because nothing was tapped. This is `IgCompat.CHECKED_OFFLINE`, and the Home screen shows it as
  "Checked offline".
- Status values in the harness reports: **OK** = hooks installed, no failure lines. **PARTIAL** = at least one sub-lookup
  failed. **FAIL** = nothing hooked. **NO-HOOK** = nothing hooked and nothing logged, usually a hook that only installs at
  runtime. **(harness)** = the failure comes from the harness's stubs, not from a lookup.
- Obfuscated names (`X.0abc`) differ between builds with **different versionNames**, even within one major version.
  The 457.0.0.55.73 splits 386314291 and 386314341 resolved to identical targets, so the split versionCode does not
  matter. The versionName does.

## Matrix

| Instagram version | Build (versionCode) | Channel | Arch | Harness result | Device-tested | Known failures | Status in app |
|---|---|---|---|---|---|---|---|
| **458.0.0.0.67** (main target) | 386405730 | alpha | not recorded | 79 steps: OK=70, PARTIAL=2, FAIL=1, NO-HOOK=6 ([report](hook-verify/report-458.0.0.0.67-386405730.md)). HideSuggestedFeedItems counts as NO-HOOK in the summary but was OK on a filtered re-run after the parser fix (section note in the report), so the effective result is OK=71, NO-HOOK=5. | No | **PlaybackSpeed: reel scrubber gate not found** (the "Force reel seek bar" switch does nothing). VoiceMessageDownload PARTIAL and LocationSpoof FAIL come from the harness. Runtime-only NO-HOOK: NotificationRegistration, JumpToFirstMessage, ReadReceiptChat, ExternalBrowser, FeedVideoDownload#install. | Checked offline |
| **457.0.0.55.73** | 386314291 ([report](hook-verify/report-457.0.0.55.73-386314291.md)), 386314341 ([report](hook-verify/report-457-hooks_b.md)) | stable | not recorded | 79 steps: OK=72, PARTIAL=1, FAIL=1, NO-HOOK=5 | No (the tester logs in `HANDOFF.md` came from older module builds) | VoiceMessageDownload PARTIAL and LocationSpoof FAIL come from the harness. Runtime-only NO-HOOK as above. | Checked offline |
| 458.0.0.0.72 | unknown | alpha | unknown | **Not run.** This is the newest public alpha (Uptodown, 2026-11-26, SHA-256 `6c79243ae4695200f92bf8f97cad609705087de905893dd02692de6f3a42b249`), but no APK could be fetched from the sandbox: APKPure, APKCombo and APKMirror return 403, and Uptodown returns 410 to non-browser clients. | No | Unknown | Unchecked build (same major) |
| 458.0.0.0.57 | unknown | alpha | arm64 | No committed report. The 0.9.0 Piko ports were written against this bytecode (`CHANGELOG.md`). | No | Unknown | Unchecked build |
| 457.0.0.6.73 | unknown | beta | unknown | Not run. The APK was never obtained (`HANDOFF.md`). | No | Unknown | Unchecked build |
| 456.x and older | n/a | n/a | n/a | Not run on 0.9.0 | No | Unknown | Older than checked |

### Coverage gap: the 0.9.0 Piko ports

The reports above were generated **before** the Piko ports landed, and they have 79 steps each. The 14 hooks added in
0.9.0 have **no offline evidence on any build**:

- FeedUfiButtons, ProfileHighlights, HideHiddenRequests and BottomSpace. These are single `if (!skip(...))` lines that the harness
  can parse, but the reports predate them.
- FollowButtons (stories and reels), LoopStory, DisappearingSwipe, ChatCallButtons, StoryTrayFilter, LikeAnimation,
  StoryRingSize, ResumeReels and MarkReadAfterReply. These are registered through `pikoInstall("Key", () -> ...)`, which the
  harness parser (`Runner.SKIP`) does not recognise yet, so they are skipped silently.

Until the reports are regenerated, treat every Piko switch as unchecked on every Instagram version.

## How the app decides

`utils/core/IgCompat.classify(versionName)`:

1. An exact match in `DEVICE_TESTED` returns **Device-tested version**.
2. An exact match in `CHECKED_OFFLINE` returns **Checked offline**.
3. A major version inside the checked range returns **Unchecked build**. A newer major returns **Newer than checked**, or
   **Alpha build, unchecked** for `x.0.0.0.y` builds. An older major returns **Older than checked**.

The "Get …" button uses `IgCompat.recommendedFor(versionName)`. It offers a checked build with the same major and channel
first, then the same major, then the same channel. Alpha users are offered the checked alpha (458.0.0.0.67) and are never
told to leave the testing programme. Everyone else is offered the checked stable build (457.0.0.55.73).

## Adding a version

1. Get the exact APK. For split bundles, use `base.apk`. Record the versionName and versionCode with
   `aapt dump badging base.apk | head -1`, and note its SHA-256 and ABI.
2. Run the checker: `IG_APK=/path/base.apk tools/hook-verify/run.sh`. Commit the report as
   `docs/hook-verify/report-<versionName>-<versionCode>.md`.
3. Read every `!!` line. Fix the real lookup failures, or list them under "Known failures" here. Only `(harness)` lines
   and runtime-only NO-HOOK steps are acceptable without explanation.
4. Add the versionName to `IgCompat.CHECKED_OFFLINE`. If it becomes the release target, also update
   `ExportBrand.TARGET_INSTAGRAM`, or `ExportBrand.STABLE_INSTAGRAM` for a stable build.
5. Add the row to this matrix. `ReleaseVersionTest` fails if a checked version is missing here, or if the CHANGELOG
   heading's target is not checked.
6. Move a version to `IgCompat.DEVICE_TESTED` only after this exact release has run on a phone with that exact
   build and the main features were used. Note who tested it and when in the matrix.
