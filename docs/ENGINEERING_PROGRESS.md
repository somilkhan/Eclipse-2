# Engineering progress

Branch `maintainer/eng-audit` → `main` via PR #22. **Do not merge until the audit is complete** (owner's instruction).

## Status (2026-11-28)

| Phase | State | Evidence |
|---|---|---|
| 1. Repo intelligence | Done | [ENGINEERING_AUDIT.md](ENGINEERING_AUDIT.md) |
| 2. Full feature audit | Partial: hooks, security, backup, downloads and compat are done; ghost-mode setting writes are reviewed (ENG-13); stories, reels and the companion UI are still open | [BUG_TRACKER.md](BUG_TRACKER.md) |
| 3. Hook verification | Done for 457.0.0.55.73 and 458.0.0.0.57 (93/93 steps). 458.0.0.0.67 is the old 79-step run; 458.0.0.0.72 is not run | [reports](hook-verify/) |
| 4. Compatibility matrix | Updated | [COMPATIBILITY_MATRIX.md](COMPATIBILITY_MATRIX.md) |
| 5. Research | Partial (APK sources, 458 bytecode changes) | this file |
| 6. Bug fixing | 27 tracked fixes on the branch, 7 open items | [BUG_TRACKER.md](BUG_TRACKER.md) |
| 7. Improvements | Not started beyond fixes | — |
| Device testing | **0%**: nothing has been run on a phone | — |
| Merged to main | **0%** (by instruction) | PR #22 open |

## Batch log

| Date | HEAD (remote verified) | Work | Tests | hook-verify |
|---|---|---|---|---|
| 2026-11-27 | (round 1 branches) | 10 fix branches, PRs #12–#21 | CI green per branch | — |
| 2026-11-28 | `562bd0b` | Integration of all 10 branches, ENG-06/07/08, 457 report regenerated (93 steps) | 588 pass | 457: OK=85 |
| 2026-11-28 | `c6dfb7a` | ENG-09 StoryTrayFilter, ENG-10 reel scrubber on 458, ENG-11 isVideo log, 458.0.0.0.57 report, matrix and IgCompat | 595 pass, 2 skipped, assembleDebug OK | 457: OK=86, 458.0.0.0.57: OK=86 |
| 2026-11-28 | (this batch) | Closed PRs #12–#21, deleted 16 merged branches, docs, ENG-13 ghost Mark as read | 600 pass, assembleDebug OK | ghost steps 10/10 OK on 457 and 458 |
| 2026-11-28 | (this batch) | ENG-14 feature-status check in hook-verify, ENG-15..20 silent failures | 601 pass, assembleDebug OK | 457/458: steps unchanged (OK=86); features HOOKED=90 BROKEN=1 SILENT=8 |
| 2026-11-28 | `2a9ff44` | Merged maintainer/root-access (ROOT-01, APP-01..07) | 629 pass, 2 skipped, assembleDebug OK | 457/458.57 identical |
| 2026-11-28 | (this batch) | First device log (458.0.0.0.67): ENG-21 LoopStory, ENG-22 DisappearingSwipe; 458.0.0.0.67 93-step report | 629 pass, assembleDebug OK | 458.0.0.0.67: OK=86; 457/458.57 identical |

## How a batch is verified

1. `./gradlew :app:testDebugUnitTest :app:assembleDebug`
2. `IG_APK=<apk> tools/hook-verify/run.sh` on every APK at hand. Diff the step statuses against the committed report; no
   step may regress.
3. Commit with the trailer, push to `maintainer/eng-audit`, and confirm with `git ls-remote origin refs/heads/maintainer/eng-audit`.
