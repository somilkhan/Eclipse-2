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

- [ ] Hooks A
- [ ] Hooks B
- [x] Features (wip/features: repo links, maintainer docs, log export, release workflow, 0.8.4/22, safe pacing)
- [ ] UI
- [x] Bugs (see docs/BUGFIXES-0.8.4.md)
- [ ] Merged, build + tests green, CHANGELOG written
- [ ] PR opened into `main`

## Things that cannot be fixed client-side on 457

(fill in as found)
