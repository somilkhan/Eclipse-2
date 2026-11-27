# InstaEclipse 0.8.4 — maintainer handoff

This file exists so work is never lost again. If a session ends (tokens, crash), a new one can
resume from GitHub alone: read this file, then the `wip/*` branches listed below.

## Goal of 0.8.4

Maintained continuation of InstaEclipse (original author: ReSo7200) at `somilkhan/Eclipse-2`.
Base: `contribute/remaining-fork-improvements` (real 0.8.1, versionCode 19).
Target Instagram: **457.0.0.55.73** (arm64 build 386314341).

| Workstream | Branch | Scope |
|---|---|---|
| Hooks A | `wip/hooks_a` | Offline DexKit verification harness against IG 457; Meta AI removal; Reels & posts (double-tap like, repost, Reels auto-scroll / scroll lock, reel scrubber, reel download menu, undo-like); feed |
| Hooks B | `wip/hooks_b` | All other hooks: follow-list export, notes tray, Save Instants, profile Saved button, media classes that moved package, max upload quality, high-res images, DMs/ghost, stories, downloads |
| Features | `wip/features` | Repo links -> somilkhan/Eclipse-2 (single constant), maintainer docs (README credit, NOTICE, MAINTAINERS.md, CONTRIBUTING.md), log export "Save to storage", release workflow + RELEASING.md, version 0.8.4 (22), safe pacing for bulk actions |
| UI | `wip/ui` | Companion app redesign: liquid-glass look, floating nav pill, Apple-quality polish |
| Bugs | `wip/bugs` | Bug audit + fixes across companion app and module, with tests |

Integration branch: `maintainer/0.8.4` (merged by the orchestrator). Final step: PR into `main`.

## Hard rules

- Never build device-fingerprint / "anti-ban" spoofing. Safe pacing (random delays between bulk actions) is fine.
- Push your `wip/*` branch to `origin` after every finished piece of work (small commits, often).
- Keep this file's "Status" section current on your branch.
- Commit trailer: `Co-authored-by: Perplexity <noreply@perplexity.ai>`.

## Status

- [x] Hooks A — harness `tools/hook-verify/` (see its README); shared helper
  `utils/core/StringTables` (+ `DexSwitchTable`) for 457's shared string tables. Harness on 457:
  baseline 79 steps OK=53 PARTIAL=10 FAIL=11 NO-HOOK=5 (`docs/hook-verify/report-457-baseline.md`)
  -> after Hooks A OK=57 PARTIAL=8 FAIL=9 NO-HOOK=5 (`docs/hook-verify/report-457-hooks_a.md`);
  every hook in the Hooks A scope is OK. All fixes are fallbacks after the old lookups.
  - LikeUndo: sender = void method with Media param using "media/%s/%s/" + "like" + "unlike" (X.0Sm8.A03).
  - ReelsControls: auto-scroll gate = static boolean(UserSession, boolean) comparing an MC string with
    "auto_scroll" (X.0AMH.A09); pager bind marker lost its "android_purge_26_q3_" prefix (X.0CBZ.E59).
    ExtrasLookup.byStrings now falls back to StringTables.
  - PlaybackSpeed: hero setter via "Before setPlaybackSpeed(), service player was evicted" (X.06r1.A0D);
    reel attached-scrubber gate via forceShowAttachedScrubber marker -> ClipsProgressController ->
    static (cfg, long, boolean)Z pair (X.0HbX.A00).
  - ReelDownload: 457 sheet adds rows one by one: Download row injected after SHARE via the row
    holder's (String, int, OnClickListener) adder, native DOWNLOAD skipped on that holder; eligibility
    gate on the "ClipsDownloadUtil.saveVideoToLocalStorage" class forced true (X.071o.A07); options-list
    patch resolves enum constants lazily.
  - RemoveMetaAI: Meta-AI MediaOption rows (GEN_AI_INFO, CONTENT_DEEP_DIVE, ...) skipped in every void
    row adder the reel sheet controller calls (6 methods on 457).
  - Unchanged and OK on 457: DisableRepost, DisableDoubleTapLike, DoubleTapExtras, ExpandCaptions,
    FeedPhotoZoom, FollowingOnlyFeed, HideCommunityNotes, HideSuggestedFeedItems.
  - Not verified on a device (no phone): row injection/click, scrubber gate semantics.
- [x] Hooks B — earlier: api.schemas video classes, profile Saved, Save Instants, follow-list export,
  notes tray, high-res images. Checker on 457.0.0.55.73: before Hooks B OK=61 PARTIAL=9 FAIL=4 NO-HOOK=5
  -> after OK=72 PARTIAL=1 FAIL=1 NO-HOOK=5 (`docs/hook-verify/report-457-hooks_b.md`). The remaining
  PARTIAL (VoiceMessageDownload compose rows) and FAIL (LocationSpoof, `Location.CREATOR`) are tagged
  `(harness)`: the lookups resolve, and the module then calls real IG or framework code the host
  can't run. The 5 NO-HOOK steps (NotificationRegistration, JumpToFirstMessage, ReadReceiptChat,
  ExternalBrowser, FeedVideoDownload.install) install lazily at runtime. All fixes are fallbacks after
  the old lookups.
  - StringTables: plain `usingStrings` also matched the table method `static String A00(int)`
    itself; `withoutTables` drops it and ExtrasLookup unions table users (fixes AccountLimit, X.0BJH.A01).
  - MaxUploadQuality: high_quality_media_upload getter via string table (X.0BWS.A00).
  - KeepUnsent / GhostEphemeralKeep: DirectThreadStore anchors via tables; ()Z ambiguity is resolved
    by picking the instance ()Z that calls Iterator.remove (X.03GI.A0Z).
  - FeedVideoDownload: isVideo falls back to "video_versions"; the Media author getter is preferred
    over the dict user getter.
  - LikeUndo: StringTables.findMethodUsingStrings fallback, then a void(Media) user of "media/%s/%s/".
  - PlaybackSpeed: hero set/prepare via "HeroManager.prepareAndMayPlay" / "HeroServicePlayer…"
    table users or a static wrapper in the same class (fixes device "prepare=null").
  - Post/Caption menus: enum constants matched by `name()`, because IG enums override `toString()`.
  - StoryMention: mentionsConverter via "StoryTappableObjectExt.mentionStickerstoReelInteractives".
  - InstantUpload: 457 photo handoff is the instance QuickSnapCameraViewModel.A1C(Context, Bitmap, …);
    the static File variant is video and stays out. Unit tests cover the binding.
  - DevOptions: a Tier 1 miss is now an info line, because Tier 2 (structural) resolves on 457.
  - Harness: dex `declared-synchronized`, real enum constants (EnumSupport), report header with
    versionName/versionCode, `(harness)` tag, `IG_APK` / `--apk` for a second build.
  - Root causes of the device and checker disagreement (device log 0.8.4-merged):
    1. The phone runs beta 457.0.0.6.73 and the checker ran 457.0.0.55.73. Obfuscated names and
       string-table placement differ per build (device: gate X.0EKw.A07, sheet X.09Cp, tray X.0emF,
       HD getter User.A18; 55.73: X.071o.A07, X.0Ckr, X.0O1f, User.A1D). An OK on 55.73 never proved
       6.73. The report header now names the exact build.
    2. The string-table self-match bug above, which made several anchors hit the wrong method.
    3. Harness gaps (synchronized flag, enum `values()`, `toString()`-overriding enums) hid real
       failures or reported false ones.
  - The merged device build 6949fcc predates every fix above (a86cc78 onward), so MaxUploadQuality,
    dictUserGetter/isVideo, LikeUndo and PlaybackSpeed prepare=null in that log are expected.
    PaidPartnershipFilter, ReelContentFilter, ProfileSpoof bio and SelectedMediaDownloads "capture
    unavailable" appear only in the log section from an older build. Those classes don't exist in
    this repo or in maintainer/0.8.4, so there's nothing to fix here.
  - OPEN: not verified against 457.0.0.6.73. That APK (a beta) couldn't be fetched from the sandbox:
    APKMirror/APKCombo returned 403, Uptodown is behind Turnstile and APKPure has no betas. Once it's
    available: `IG_APK=~/ig/ig457_6.apk tools/hook-verify/run.sh`, then commit the report as
    `docs/hook-verify/report-457-6-hooks_b.md`.
- [x] Features (wip/features: repo links, maintainer docs, log export, release workflow, 0.8.4/22, safe pacing)
- [x] UI (wip/ui: liquid-glass redesign of the companion app; see "UI notes" below)
- [x] Bugs (see docs/BUGFIXES-0.8.4.md)
- [x] Merged into `maintainer/0.8.4`, `assembleDebug` + `testDebugUnitTest` (418 tests, 0 failures) + `lintDebug` (0 errors) green, CHANGELOG written
- [ ] PR opened into `main`

## UI notes
- iOS 26 "Liquid Glass" redesign (wip/ui). Glass is used only for chrome (tab bar, top scroll edge, back/search controls, dialogs); content cards are opaque grouped-inset rows on #F2F2F7 / black. Single accent: indigo #5856D6 (dark #8B89FF).
- Glass components in `app/src/main/java/io/github/somilkhan/instaeclipse2/ui/glass/`:
  - `LiquidSupport` decides whether the `com.github.QWEA0:liquidglass` library may run (API 33+, native `nativegauss` loads, not battery saver / high contrast, not `-Dinstaeclipse.softBlur`/`noLiquid`). Any library failure calls `markBroken` and everything falls back to `BackdropBlurLayout`.
  - `SafeLiquidGlassView` wraps the library `LiquidGlassView` and catches `Throwable` while drawing (the library only catches `Exception`, but a missing `.so` throws `UnsatisfiedLinkError`).
  - `GlassNavPill` is our own tab bar (TalkBack tabs, RTL, haptics). It uses the library glass body plus a clear droplet lens when available, otherwise the in-house glass. It shrinks to a compact capsule when you scroll down and expands when you scroll up or tap it; it doesn't shrink with TalkBack and skips the animation with reduce motion. We don't use the library's `LiquidGlassTabBar` because it swallows every touch, has no accessibility and has an RTL index bug.
  - `ScrollEdgeEffect` is the top scroll-edge blur: the library `ScrollEdgeBlurView` plus a tint on API 31+, and an edge-fade `BackdropBlurLayout` below that. Used by `MainActivity` (all tabs) and `FeaturesFragment` (behind the title and search field).
  - `BackdropBlurLayout`: RenderNode blur (API 31+) or CPU blur (28–30), plus the approximated rim refraction (chromatic split), specular rim and edge-fade mode. It turns opaque in high contrast.
  - `IosSwitch`: 51x31dp, green #34C759 / #30D158, spring thumb, and the knob stretches into translucent glass while it's pressed. It extends `MaterialSwitch`, so listeners and `isChecked` are unchanged.
- Type: Inter (OFL) as font resources (`res/font/inter*`). The type scale lives in `styles.xml` (`TextAppearance.IE.*`). Icons are Phosphor regular/fill vectors `ic_ui_*` (MIT). The licences are in `third-party/` and `NOTICE`. The in-IG `ic_*` drawables stay as they were (`IconMap` maps old ids to new ones in the companion app).
- Edge-to-edge insets are applied by `MainActivity` via view tags on fragment roots: `ie_scroll`, `ie_scroll_list`, `ie_large_title`, `ie_inset_top`, `ie_inset_bottom`, `ie_inset_bottom_margin`, `ie_float_above_nav` (guarded by `InsetOverlapTest`).
- Screenshots: `./gradlew testDebugUnitTest --tests '*ScreenshotTest*' -Pie.screenshotSet=final` writes `docs/screenshots/0.8.4/final/`. Robolectric can't run the native/AGSL library, so the screenshots show the in-house CPU fallback. Real lens refraction, the library scroll-edge blur, the drop shadow, the blur behind dialogs and the animations only show on a device. `HomeFragment.autoScrollEnabled` is switched off in the tests so the carousels are captured at rest.
- In-Instagram UI (`mods/**`) untouched.

## Things that cannot be fixed client-side on 457

- Reel download "restricted viewer" check (Hooks A): on 457 it is an inline MobileConfig read
  (`X.000H.A1E(ctx, 0x81030600060b90L)`) inside the sheet builders, no dedicated method. Hooking the
  generic MC getter would hit every config read. Our injected row in the classic sheet skips this check;
  the native row in the newer sheet still honours it.
- Meta AI surfaces whose "android_purge_*" markers were removed in 457 and have no stable replacement
  anchor yet (Hooks A, open, not proven impossible): in-feed Reels Meta AI card
  (MetaAiClipsEligibilityFetcher), Meta AI video attribution subtitle, reel ⋮ header "Ask Meta AI"
  entrypoint gate (old MC ids gone), search SERP Meta AI HCM card, Content Deep Dive prompt/fetcher.
  The Meta AI rows inside the reel ⋮ sheet are removed (row filter). Leads: "meta_ai_in_reels_unit",
  "ig_reels:meta_ai_contextual_entrypoint_overflow_menu" (in X.0Ckr.A0C), "TopSerpMetaAIHCMFragment",
  "MetaAiContentDeepDiveQuery".
