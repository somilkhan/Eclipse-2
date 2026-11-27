# Changelog

## 0.8.4 (versionCode 22), for Instagram 457.0.0.55.73

First release of Eclipse 2, the maintained continuation at `somilkhan/Eclipse-2` (original author: ReSo7200).
Built on 0.8.1 (`contribute/remaining-fork-improvements`).

### Instagram 457 hook fixes
- New offline hook checker (`tools/hook-verify/`) that runs every hook lookup against an IG APK with DexKit.
  On 457: baseline OK=53 / PARTIAL=10 / FAIL=11, and after these fixes OK=57 / PARTIAL=8 / FAIL=9
  (`docs/hook-verify/`). Re-run on build 386314291 after Hooks B: OK=72 / PARTIAL=1 / FAIL=1 / NO-HOOK=5,
  both remaining failures are harness stubs (`docs/hook-verify/report-457.0.0.55.73-386314291.md`).
- Shared `StringTables` / `DexSwitchTable` helpers for 457's shared string tables.
- Undo like, Reels auto-scroll / scroll lock, playback speed and reel scrubber, reel download menu row,
  Meta AI rows in the reel ⋮ sheet.
- Video-version model classes that moved to `api.schemas`, Saved button on profile, Save Instants
  (one save per long press), follow-list export, hide notes tray, high-res images.
- All fixes are fallbacks after the old lookups, so older Instagram versions keep working.

### Features
- Logs: "Save to storage" (full log with a diagnostic header), live incremental view, no cut-off.
- Safe pacing for bulk actions (random delays, on by default).
- Repo links point at `somilkhan/Eclipse-2` through one `BuildConfig.GITHUB_REPO` constant.
- Maintainer docs: README credit, NOTICE, MAINTAINERS.md, CONTRIBUTING.md, RELEASING.md.
- Release automation: signed APK + GitHub Release on `vX.Y.Z` tags.

### Bug fixes
- 15+ fixes across downloads, backup/restore, lifecycle, location picker, root restart and i18n,
  with new unit tests. Full list: `docs/BUGFIXES-0.8.4.md`.

### UI
- New companion app look: glass tab bar and top bar, solid grouped cards, iOS-style switches, Inter font, thin line icons, eight theme presets and a rebuilt Home screen.

### Performance (companion app)
- Tabs are created once and then shown or hidden instead of being rebuilt on every tap. The other
  tabs are built in the background after the first frame, so the first switch doesn't stutter.
- Tab switch uses a short 180 ms fade on a hardware layer (skipped when animations are reduced).
- Glass tab bar: the glass library redrew the whole screen behind the pill on every frame, even at
  rest. The backdrop is now refreshed only when the content behind it actually changes, and the
  sliding droplet no longer triggers a window layout on every spring frame.
- Home auto-scroll runs on `Choreographer` frames with sub-pixel steps, so it's smooth at 90/120 Hz.

### Old module conflict
- The log now uses its own file (`eclipse2_module.log`). Before, it read `instaeclipse_module.log`,
  which the original InstaEclipse also writes inside Instagram, so old failures showed up in our log.
- Each Instagram start writes a session line with the module and Instagram version.
- If the old module is still active in the same Instagram process, the log says so. The companion
  app shows a dialog when `ps.reso.instaeclipse` is installed.
- Post menu Download: when the strict media lookup misses, it falls back to the tapped menu's own
  Media, and the log line says why the lookup missed.

### Rename and branding
- The project is now Eclipse 2. The package name changed from `ps.reso.instaeclipse` to `io.github.somilkhan.instaeclipse2`, so it installs beside the original and settings do not migrate.
- New app icon and logo. The old ones copied Instagram's look and were removed, along with the Instagram glyph inside the app.
- Disclaimer now states that no Instagram code or files are included.

### Known limits
- See "Things that cannot be fixed client-side on 457" in `docs/HANDOFF.md`.
- Not yet tested on a real device.
