# InstaEclipse 0.8.4 — maintainer handoff

This file exists so work is never lost again. If a session ends (tokens, crash), a new one can
resume from GitHub alone: read this file, then the `wip/*` branches listed below.

## Goal of 0.8.4

Maintained continuation of InstaEclipse (original author: ReSo7200) at `somilkhan/InstaEclipse`.
Base: `contribute/remaining-fork-improvements` (real 0.8.1, versionCode 19).
Target Instagram: **457.0.0.55.73** (arm64 build 386314341).

| Workstream | Branch | Scope |
|---|---|---|
| Hooks A | `wip/hooks_a` | Offline DexKit verification harness against IG 457; Meta AI removal; Reels & posts (double-tap like, repost, Reels auto-scroll / scroll lock, reel scrubber, reel download menu, undo-like); feed |
| Hooks B | `wip/hooks_b` | All other hooks: follow-list export, notes tray, Save Instants, profile Saved button, media classes that moved package, max upload quality, high-res images, DMs/ghost, stories, downloads |
| Features | `wip/features` | Repo links -> somilkhan/InstaEclipse (single constant), maintainer docs (README credit, NOTICE, MAINTAINERS.md, CONTRIBUTING.md), log export "Save to storage", release workflow + RELEASING.md, version 0.8.4 (22), safe pacing for bulk actions |
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
- [ ] Hooks B
- [x] Features (wip/features: repo links, maintainer docs, log export, release workflow, 0.8.4/22, safe pacing)
- [ ] UI
- [x] Bugs (see docs/BUGFIXES-0.8.4.md)
- [ ] Merged, build + tests green, CHANGELOG written
- [ ] PR opened into `main`

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
