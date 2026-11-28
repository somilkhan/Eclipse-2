# Changelog

## Unreleased

### Features
- Root access (opt-in): Features → Tools → Root access. "Use root (if available)" is off by
  default and is not included in backups. The status shows not rooted / denied / granted, and the
  check runs off the main thread with a 30 s timeout. When root is on and granted, three actions use
  it: restart Instagram (`am force-stop`), clear the DexKit hooks cache in every installed supported
  Instagram, and collect crash reports Instagram could not deliver. The Logs tab can also read the
  module log straight from Instagram's data folder when its own copy is missing. Every action keeps
  the old non-root path as a fallback, and the result message says which path was used and whether
  root was tried and failed. Package names come from a fixed whitelist and are quoted, file names
  are checked against a strict pattern, and there is no root IPC for other apps. Not tested on a
  device yet.

### App fixes
- Home → Restart Instagram ran `su` on every device, even unrooted ones without asking, ignored its
  result and waited a fixed 400 ms. It now uses root only when opted in, and otherwise opens App info.
- Features → Tools → Restart App only showed a toast. It now restarts Instagram with root, or opens
  App info so you can force stop it.
- Resetting the download or crash report folder kept the old folder's permission forever. It is now
  released unless the other setting still uses the same folder.
- Crash reports: a failed write left an empty report file in the folder and failures were not logged.
  The empty file is removed and every failure is logged. Very long reports are no longer cut in the
  middle of an emoji.
- Logs → Clear logs was sent to any app with an Instagram package name, also untrusted ones. It now
  only goes to trusted Instagram builds, like every other command.
- Logs showed a 3 second spinner even when no Instagram was asked for logs.
- Home: the enabled features count missed features that are on by default (e.g. fingerprint
  unlock), and pinned distraction shortcuts stayed locked after a Focus Lock had run out.

### Fixes
- Ghost mode: the DM and channel Mark as read buttons switched Ghost seen back on if it had been
  turned off with the quick toggle while the chat was open, and could overwrite a toggle made in
  the same moment. They now open a short "let seen through" window without touching the setting,
  and the window closes by itself after 2 seconds if anything goes wrong.
- Reels → Force reel seek bar did nothing on Instagram 458 ("reel scrubber gate not found").
  On 458 the `(config, duration, isLong)` wrapper that picked the "show scrubber" flag was inlined
  into its callers, so there was nothing left to hook. The module now finds the config's
  show-flags (the booleans every inlined caller reads beyond the shared duration check) and sets
  them on every config Instagram builds. 457 keeps the old gate hook. Checked offline on
  458.0.0.0.57; not tested on a device yet. Reels opened before the switch is turned on keep the
  old config.
- Download log on 458 reported `isVideo=not found` although video detection works there through
  the hashed `video_versions` accessor. The log now names the path that is used.
- Story follow button (FollowButtons) and the story tray filter failed to install on 457.0.0.55.73
  (one component's name lookup, one enum `toString()` that throws). Both now resolve offline.
- Feed → Custom like animation did nothing. Only `LikeActionView` was hooked, but Instagram picks
  the like animation from `XDTUserActivationMetadataImpl(animation, Boolean enabled, ...)` and
  plays the plain heart when it is missing or disabled. Its constructor now gets the picked
  animation and `enabled = TRUE`, as in Piko, and the view uses Instagram's own metadata → render
  mapping so both agree. Random picks one animation per app start for both. The plain heart
  (`ARES_LIKE_ACTIVATION`) is no longer offered as a "custom" pick, and names spelled differently
  in the two enums (AKI_KOICHI / AKI_KOCHI) are matched. The log now shows which layers hooked and
  the first animation applied.

## 0.9.0 (versionCode 23), for Instagram 458.0.0.0.67

Clone app support, automatic crash reports, the story download fix for Instagram 458 and 19
new switches ported from Piko. Every new Piko switch is off by default.

> Not yet tested on a device. All of this passed CI (unit tests and build) and the hook points were
> checked against Instagram 458.0.0.0.57 bytecode. If something breaks, attach the report from the
> `Crash reports` folder (on by default) to an issue.

### Fixes
- Story download crash on 458: the Story photo lookup walked up to 7 levels of the selected
  Media's object graph on the main thread and called every no-arg String/int/long method it met.
  On 458 those are JNI-backed Pando getters, and Instagram crashed at the Download tap (posts and
  reels never used this walk). Story and highlight lookups now read fields only; the method probe
  stays for voice-message thumbnails, where it is still needed.

### Supported apps
- Added InstaAlpha (`com.instaalpha.android`) next to Instafel (`com.instafel.android`). The
  unclone builds of both use `com.instagram.android` and were already covered.
- LSPosed scope (`xposedscope`) now lists every supported package instead of only
  `com.instagram.android`, so clones are pre-selected when the module is enabled.
- DM conversation menu hook no longer checks for `com.instagram.android` only.
- Trusted clones: App settings → "Clone apps" lists installed clones. Trusting one pins its
  current signing key (SHA-256) in the companion's private prefs (not synced, not backed up).
  A trusted clone gets live settings sync both ways, downloads into the companion folder and
  config import; before, all of these were official-Instagram-only ("Companion request denied").
  A different signer under the same package name is denied until trusted again.

### Crash reports
- New "Crash reports" section (App settings, default on). When a hooked Instagram crashes, the
  module writes a report (type, time, app/module/device/Android versions, stack trace, last 400
  Eclipse 2 log lines) and the companion saves it as `Crash reports/crash_<pkg>_<time>_<kind>.txt`
  in the chosen folder, else the download folder, else `Download/Eclipse 2/Crash reports`.
- Java crashes are caught live (`Thread.dispatchUncaughtException`). Native crashes and ANRs are
  read on the next start from `ApplicationExitInfo` (Android 11+); on Android 12+ the native
  tombstone is decoded (signal, abort message, crashing-thread backtrace) and the raw
  `.tombstone.pb` is saved next to it. Undelivered reports stay pending and are retried.

### Settings
- Ghost Mode master switch: `isGhostModeEnabled` was never tied to the "Enable/disable all"
  switch, so it was always saved and synced as `false`, even with every Ghost Mode feature on. It
  now mirrors that switch (on only while all eight Ghost Mode features are on), is recomputed on
  every settings load and save, and a stale stored value from older builds is corrected.

### Ported from Piko
Features from [Piko](https://github.com/crimera/piko) (GPL-3.0) that Eclipse 2 did not already
have, rewritten as Xposed hooks. The hook points were checked against the bytecode of
Instagram 458.0.0.0.57 (arm64). They have not been tested on a device yet. All switches are off
by default, and each hook installs nothing and logs a reason when its target is missing.
- Feed → Hide share button / save button / repost button / like count on posts. These clear the
  matching flags in the feed post state (`MediaUfiUiState`, X.04d9 on 458) after its constructor
  runs. The obfuscated fields are matched to their `toString` labels on the first post by
  flipping each boolean once and restoring it. A label that does not map to exactly one field is
  left alone. Feed only: on 458 the reels checks are inlined into larger methods. "Hide repost
  button" only hides the button; "Disable repost" still blocks the repost action.
- Feed → Hide highlights on profiles: hides `highlights_tray` after
  `ProfileHeaderBinderGroup.bindView`, and gives back Instagram's own visibility before the next
  bind.
- Messages → Hide "Hidden requests": collapses the `pending_threads_spam_folder_row` row when it
  is inflated.
- Navigation → Remove empty space under the tab bar: the nav bar size method (strings
  `config_showNavigationBar` + `_hasNavigationBar_notFound`, one caller, the insets listener)
  returns 0, Instagram's own "no nav bar" value. Takes effect after Instagram restarts.
- Not ported, already in Eclipse 2: copy comment, custom font, share domain, nav bar
  customisation, reels scroll lock, ads, comments, discover people, double-tap like, explore,
  screenshot detection, stories, story flipping, swipe to camera, typing, autoplay, downloads,
  follows-you label, hidden chats, feed, create group on share sheet, notes tray, stories tray,
  suggested content, high-res images, inbox lock, following-only feed, permanent view-once,
  mark as read, links externally, tracking links, Instants, unsent messages, comment media,
  theme, dev options, ghost mode, story mentions, build-expired popup, notification fix,
  exact timestamps.
- Not ported: reels share/save/like. On 458 those checks are inlined into larger methods.

### Ported from Piko (batch 2)
The features the first batch skipped, done with method hooks instead of bytecode edits. Hook
points come from Instagram 458.0.0.0.57 (arm64). They are not device-tested yet, are off by
default, and each one logs and skips when its target is missing.
- Stories → Hide Follow button: hides the follow holders in the story header binder and the
  `StoryFollowButtonViewComponent` views.
- Reels → Hide Follow button: `FollowButtonBase` views inside a `clips_*` container stay hidden.
- Stories → Loop stories: the "end of scene" path replays the current item, and the video player
  seeks to 0 and resumes, instead of moving on.
- Messages → Disable swipe up for disappearing messages: the swipe gate returns false unless
  vanish mode is already on, so you can still swipe out of it.
- Messages → Hide audio and video call buttons: removes the call kinds (`*_CALL`,
  `COMBINED_CALL_*`, `COLLAPSED_CALL`) from the thread header button list.
- Stories → Filter stories tray: drops tray items by reel type and by verified or unverified
  account. Piko's min/max media count filter is not ported.
- Feed → Custom like animation: `LikeActionView.setUpCustomLikesAnimation` with a picked style or
  a random one each time. `UNRECOGNIZED` is never used.
- Stories → Custom story ring size (40–160%): replaces the 66/100 dp sizes in the tray item
  constructor. Needs an Instagram restart.
- Reels → Resume where you left off: saves the position when a reel goes out of view and seeks
  there on the next prepare. Positions under 1 s are not saved.
- Post menu → Open in external downloader: a post/reel menu row (`SHOP_LANE_DEBUG` carrier,
  `DEBUG_AD` fallback) that sends the `instagram.com/p/` or `/reel/` link to the set app, or to
  the share chooser when none is set or that app is missing.
- Messages → Mark chat as read when you reply: with Ghost seen on, the newest blocked read
  receipt per chat is held and sent with Instagram's own sender once a message you sent in that
  chat is confirmed. It does nothing with Ghost seen off.
- Distraction-free → Focus Lock: Extreme Mode with an end time (1 minute to 3 months), in the
  in-app dialog and the companion app. When the time is up, the switches unlock and keep their
  state. Moving the device clock forward ends the lock early.

### Known limits
- Reels share/save/like hiding and Piko's story-count tray filter are not ported (see #8).
- Native crash and ANR reports need Android 11+ and are saved on the next Instagram start.
- New settings text is English only for now.

## 0.8.4 (versionCode 22), for Instagram 457.0.0.55.73

First release of Eclipse 2, the maintained continuation at `somilkhan/Eclipse-2` (original author: ReSo7200).
Built on 0.8.1 (`contribute/remaining-fork-improvements`).

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
