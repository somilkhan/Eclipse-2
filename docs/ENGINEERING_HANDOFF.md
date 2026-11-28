# Engineering handoff

Recorded 2026-11-28. Supersedes nothing: `docs/HANDOFF.md` is the historical 0.8.4 handoff.

## Where things are

- **Integration branch**: `maintainer/eng-audit`, which carries all 10 round-1 fix branches plus the round-2 fixes. PR #22 → `main`.
  The owner asked for one PR and a merge only when the whole audit is done.
- **Superseded PRs**: #12–#21 were closed on 2026-11-28 at the owner's request. Their 10 `fix/*` branches, 5 `wip/*` branches and
  `contribute/remaining-fork-improvements` were deleted, but only after checking that each one is an ancestor of `maintainer/eng-audit`.
  `wip/ui-liquid-snapshot` (Liquid Glass UI WIP) is **not merged** and was kept.
- **Versions**: still 0.9.0 / versionCode 23. Changes are under `## Unreleased` in `CHANGELOG.md`.

## Toolchain notes

- JDK 17, Android SDK platforms 34/36, build-tools 34.0.0. Set `ANDROID_HOME` for `tools/hook-verify/run.sh` (it defaults
  to `~/android-sdk`).
- Maven Central rate-limits (429) some sandboxes. A Gradle init script that adds Google's Maven mirror first works around
  it; don't commit it.
- hook-verify needs the host `libdexkit` for DexKit 2.0.7 and the APK's `base.apk`. Refresh the classpath with
  `./gradlew -I tools/hook-verify/classpath.init.gradle :app:hookVerifyClasspath`, then run
  `IG_APK=... tools/hook-verify/run.sh --skip-gradle [--filter REGEX]`.
- APKs: APKMirror serves 457.0.0.55.73 and 458.0.0.0.57 through a real browser. 458.0.0.0.67 and .72 were not available there.

## Risks

1. Nothing is device-tested. The highest-risk fixes are ENG-10 (reel scrubber: it writes Instagram config fields),
   SEC-02 (share-download proof: a mistake blocks share downloads) and MEDIA-01..04 (all legacy saves now go through a new path).
2. ENG-10 forces flags only on configs built after the switch is on, so open reels keep the old state.
3. 458.0.0.0.67, the release target, has no fresh 93-step report.

## Next tasks (priority order)

1. Device test on 458.0.0.0.x and 457.0.0.55.73: reel seek bar, share download, story/highlight download, backup/restore,
   ghost view-once, story follow button, story tray filter.
2. Get the 458.0.0.0.67 and 458.0.0.0.72 APKs and run hook-verify. Add them to the matrix.
3. Behavioural audit of ghost-mode switch interactions, stories/reels playback, and the companion UI. Triage issue #8.
4. Harness: stub `Location.CREATOR` and the voice-message path, so that those two steps reflect real lookups.
5. Re-audit the round-1 IDs that were not recovered (listed in BUG_TRACKER.md).
