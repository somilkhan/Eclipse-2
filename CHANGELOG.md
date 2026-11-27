# Changelog

## 0.8.4 (versionCode 22), for Instagram 457.0.0.55.73

First release of Eclipse 2, the maintained continuation at `somilkhan/Eclipse-2` (original author: ReSo7200).
Built on 0.8.1 (`contribute/remaining-fork-improvements`).

### Settings
- Ghost Mode master switch: `isGhostModeEnabled` was never tied to the "Enable/disable all"
  switch, so it was always saved and synced as `false`, even with every Ghost Mode feature on. It
  now mirrors that switch (on only while all eight Ghost Mode features are on), is recomputed on
  every settings load and save, and a stale stored value from older builds is corrected.

### Instagram 458 (458.0.0.0.67) hook fixes
- Instagram 458 deleted the typed per-field getters on its models (Media went from 1368 to 502
  methods) and renamed `com.instagram.user.model.User` to an obfuscated class. Each model still has
  one generic `Object x(int)` accessor keyed by the JSON field name's `String.hashCode()`. New
  `IgModels` helper finds the User class by its `XDTUserDict` type name and reads fields by hash.
- Fallbacks on top of the old lookups for: hide liked posts, "Follows you" badge, story mentions,
  story links, story info, caption copy, feed/reel download (carousel, audio, DASH, username,
  video_versions), HD profile picture, force reel quality, following-only feed, story auto-advance,
  hero playback speed. All User checks now go through `IgModels`.
- Post/reel menu download, copy link and media info: on 458 the menu creator no longer holds a
  Media field, so taps showed "Media not found". The click handler is now read the way Instagram
  reads it: its own `static Media x(Handler)` accessor (the reel handler's "clipsItem" path), a
  direct Media field, or an already-initialized `Lazy<Media>`. A Lazy is never forced, and two
  different Media values still fail closed.
- Download capture on 458: Media has no `getId()` and no typed field getters any more, so the
  snapshot stopped at "Media identity unavailable". Fields the adapter cannot bind now fall back to
  the model's own hashed accessor (id, user, video_versions, image_versions2, candidates, ...).
  `IgModels` also accepts the synthetic `x(int)` accessor that Pando models (image info, video
  versions) declare, but never on a List or Map. The capture error log now includes its reason.
- First launch after an install or update: the background DexKit pass used every core while
  Instagram was still starting, and Android killed it with "failed to complete startup" (ANR). It
  now starts after Instagram's startup has finished, 1.5 s later, at minimum priority, using half
  the cores.
- Cached launches: hook installation ran inside `Application.attach` on the main thread and took
  about 22 s on 458, because some lookups cannot be cached (media adapter, lookups with no result).
  It now runs on a background thread. Instagram waits at most 1.5 s for it, so the main-activity
  and startup-tab hooks still go in first, and the rest finish while the feed loads.
- Hide suggested posts: the FeedItem parser lookup matched Instagram's shared string table
  (`X.0000`) on 458 and hooked nothing. It now goes through `StringTables` and only accepts a class
  that has the parser bridge.
- Offline checker on 458 build 386405730: OK=71 / PARTIAL=2 / FAIL=1 / NO-HOOK=5 (baseline was
  OK=60 / PARTIAL=6 / FAIL=8). Remaining: reel scrubber gate (PlaybackSpeed, optional) and two
  harness-only stubs. 457 is unchanged at OK=72 / PARTIAL=1 / FAIL=1.
- The "first launch for this Instagram version" log line now says why (cache reset vs. previous run
  unfinished), and a hook that throws out of the background pass no longer keeps the cache from
  being marked complete.

### Home screen
- Instagram compatibility row: shows whether the installed Instagram is the tested build, another
  build of the same version, newer, an alpha/beta, or older, with a link to the tested APK.
- Credits: maintained by @somilkhan, original by @reso7200 (About dialog, footer, in-app menu,
  contributors list).

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
