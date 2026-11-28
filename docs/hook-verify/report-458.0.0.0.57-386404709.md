# hook-verify report

APK: `ig458-57.apk` — Instagram 458.0.0.0.57 (386404709)

Results hold for this exact build only; a device running another build of the same major version can resolve different classes.

Status legend: **OK** = hooks installed and no failure lines; **PARTIAL** = some hooks installed but at least one sub-lookup logged a failure; **FAIL** = failure logged and nothing hooked; **NO-HOOK** = nothing hooked and nothing logged (usually a hook that installs lazily at runtime, or one that only hooks framework classes once an Activity exists); **ERROR** = install threw. **(harness)** = every failure line comes from the offline harness (stubbed Instagram code or mockable android.jar), not from a lookup; confirm on a device.

```
OK       instagramUI                      hooks=5      518 ms
OK       DevOptionsUnlockHook             hooks=1      961 ms
OK       GhostDMSeenHook                  hooks=1      162 ms
OK       GhostDMMarkAsReadHook            hooks=1       52 ms
OK       GhostChannelMarkAsReadHook       hooks=1       53 ms
OK       GhostTypingIndicatorHook         hooks=1      156 ms
OK       GhostScreenshotDetectionHook     hooks=1      142 ms
OK       ScreenshotPermissionHook         hooks=2       53 ms
OK       GhostViewOnceHook                hooks=1      160 ms
OK       GhostStorySeenHook               hooks=6      151 ms
OK       KeepUnsentMessagesHook           hooks=7      726 ms
OK       UnsentThreadButtonHook           hooks=5       58 ms
OK       HideChatsHook                    hooks=3      260 ms
OK       CustomFontHook                   hooks=9       79 ms
OK       metaAi                           hooks=2       57 ms
OK       metaAi.reels                     hooks=8     2227 ms
OK       DisableRepostHook                hooks=2      361 ms
OK       LockDirectMessagesHook           hooks=1       52 ms
OK       HideSuggestedFeedItemsHook       hooks=2      595 ms
OK       AdBlocker                        hooks=1      156 ms
OK       TrackingLinkDisable              hooks=5       55 ms
OK       ExactTimestampHook               hooks=1      121 ms
OK       ExpandCaptionsHook               hooks=2      176 ms
OK       HideCommunityNotesHook           hooks=2       63 ms
OK       ReelsControlsHook                hooks=3      770 ms
OK       AccountLimitHook                 hooks=1      406 ms
OK       AirplaneModeHook                 hooks=3       54 ms
OK       HighResolutionImageHook          hooks=1      178 ms
OK       OnboardingPromptHook             hooks=1      143 ms
NO-HOOK  NotificationRegistrationHook     hooks=0       50 ms
OK       StartupTabHook                   hooks=4       52 ms
OK       SwipeToCameraHook                hooks=1       53 ms
OK       FollowingOnlyFeedHook            hooks=5      258 ms
OK       ProfileFollowsYouHook            hooks=2      152 ms
OK       PlaybackSpeedHook                hooks=25    2162 ms
OK       HideNotesTrayHook                hooks=1      184 ms
OK       FeedUfiButtonsHook               hooks=1       68 ms
OK       ProfileHighlightsHook            hooks=1       65 ms
OK       HideHiddenRequestsHook           hooks=1       52 ms
OK       BottomSpaceHook                  hooks=1      154 ms
OK       FollowButtonsHook.stories        hooks=1      248 ms
OK       FollowButtonsHook.reels          hooks=3       52 ms
OK       LoopStoryHook                    hooks=2      121 ms
OK       DisappearingSwipeHook            hooks=2      483 ms
OK       ChatCallButtonsHook              hooks=1      127 ms
OK       StoryTrayFilterHook              hooks=1      214 ms
OK       LikeAnimationHook                hooks=2       55 ms
OK       StoryRingSizeHook                hooks=2       71 ms
OK       ResumeReelsHook                  hooks=2      158 ms
OK       MarkReadAfterReplyHook           hooks=1      259 ms
PARTIAL  VoiceMessageDownloadHook         hooks=5      573 ms
OK       EditHistoryHook                  hooks=2      220 ms
NO-HOOK  JumpToFirstMessageHook           hooks=0       52 ms
NO-HOOK  ReadReceiptChatHook              hooks=0       52 ms
OK       StoryViewerSearchHook            hooks=1      262 ms
OK       CommentSearchHook                hooks=2      430 ms
OK       ChatExportHook                   hooks=1      312 ms
OK       FollowListExportHook             hooks=1      312 ms
OK       LikeUndoHook                     hooks=1     1118 ms
OK       ShareSheetCleanupHook            hooks=2       55 ms
NO-HOOK  ExternalBrowserHook              hooks=0       51 ms
OK       DisableStoryFlippingHook         hooks=1       83 ms
OK       StoryMentionHook                 hooks=11     501 ms
OK       StoryLinksHook                   hooks=11     330 ms
OK       StoryInfoHook                    hooks=11    1504 ms
OK       CommentCopyHook                  hooks=4      207 ms
OK       CaptionCopyContextMenuHook       hooks=11    1151 ms
OK       DisableDoubleTapLikeHook         hooks=2      770 ms
OK       DoubleTapExtrasHook              hooks=6      304 ms
OK       FeedPhotoZoomHook                hooks=1       54 ms
FAIL     LocationSpoofHook                hooks=0       52 ms  (harness)
OK       IgThemeHook                      hooks=21      66 ms
OK       ForceReelQualityHook             hooks=11     107 ms
OK       MaxUploadQualityHook             hooks=1      410 ms
OK       DisableVideoAutoPlayHook         hooks=9      169 ms
OK       BuildExpiredPopupHook            hooks=2      273 ms
NO-HOOK  FeedVideoDownloadHook            hooks=0       55 ms
OK       FeedVideoDownloadHook.videoUrlCapture hooks=2     2252 ms
OK       PostDownloadContextMenuHook      hooks=5      361 ms
OK       InstantSaveHook                  hooks=2       54 ms
OK       InstantUploadHook                hooks=10      55 ms
OK       GhostEphemeralKeepHook           hooks=6      406 ms
OK       GhostPermanentViewHook           hooks=1      169 ms
OK       ViewOnceBadgeHook                hooks=1       80 ms
OK       LockedCollectionsHook            hooks=1       51 ms
OK       SavedOnProfileHook               hooks=1       67 ms
OK       HideHeaderButtonsHook            hooks=1       52 ms
OK       ShareLinkDownloadHook            hooks=3       52 ms
OK       StoryDownloadHook                hooks=16    1389 ms
OK       ReelDownloadHook                 hooks=5      482 ms
OK       ProfilePicDownloadHook           hooks=2       68 ms
OK       interceptor                      hooks=2       65 ms
OK       TerminatedExecutorGuard          hooks=1       51 ms
TOTAL: 93 steps  OK=86 PARTIAL=1 FAIL=1 NO-HOOK=5 ERROR=0
```

## Feature status (FeatureStatusTracker)

Every feature registered by `FeatureManager.refreshFeatureStatus()` with all flags on, after all steps. **HOOKED** = the feature reported its hook; **BROKEN** = it reported a failed lookup (shown red in the load toast); **SILENT** = it reported neither, so on a device the load toast cannot say whether it works. SILENT is not proof of a failure: runtime-only hooks report later.

HOOKED=90 BROKEN=1 SILENT=8

| Feature key | State |
|---|---|
| `AdBlocker` | HOOKED |
| `AllowScreenshots` | HOOKED |
| `BulkUnsend` | HOOKED |
| `CacheStories` | HOOKED |
| `CaptionCopy` | HOOKED |
| `ChatExport` | HOOKED |
| `ChatTranslate` | BROKEN |
| `CommentSearch` | HOOKED |
| `CopyComment` | HOOKED |
| `CopyMediaLink` | HOOKED |
| `CustomEmoji` | HOOKED |
| `CustomFont` | HOOKED |
| `CustomLikeAnimation` | HOOKED |
| `CustomStoryRingSize` | HOOKED |
| `CustomTheme` | HOOKED |
| `DevOptions` | HOOKED |
| `DisableDisappearingSwipe` | HOOKED |
| `DisableDiscoverPeople` | SILENT |
| `DisableDoubleTapLike` | HOOKED |
| `DisableHighlights` | HOOKED |
| `DisableRepost` | HOOKED |
| `DisableTrackingLinks` | HOOKED |
| `DmMediaDownload` | HOOKED |
| `EditHistory` | HOOKED |
| `ExactTimestamps` | HOOKED |
| `ExpandCaptions` | HOOKED |
| `ExternalDownloader` | HOOKED |
| `FancyBioFont` | SILENT |
| `FilterStoryTray` | HOOKED |
| `FollowListExport` | HOOKED |
| `FollowerToast` | SILENT |
| `FollowersHistory` | HOOKED |
| `FollowingOnlyFeed` | HOOKED |
| `GhostLive` | SILENT |
| `GhostScreenshot` | HOOKED |
| `GhostSeen` | HOOKED |
| `GhostStories` | HOOKED |
| `GhostTyping` | HOOKED |
| `GhostViewOnce` | HOOKED |
| `HideChatCallButtons` | HOOKED |
| `HideCommunityNotes` | HOOKED |
| `HideDmBlend` | HOOKED |
| `HideFeedLikeCount` | HOOKED |
| `HideFeedRepostButton` | HOOKED |
| `HideFeedSaveButton` | HOOKED |
| `HideFeedShareButton` | HOOKED |
| `HideHiddenRequests` | HOOKED |
| `HideLikedPosts` | HOOKED |
| `HideNotesTray` | HOOKED |
| `HideReelsFollowButton` | HOOKED |
| `HideShareCreateGroup` | HOOKED |
| `HideSpecificChats` | HOOKED |
| `HideStoryFollowButton` | HOOKED |
| `HideSuggestionsInFeed` | HOOKED |
| `HideThreadsButton` | HOOKED |
| `HideThreadsSuggestions` | HOOKED |
| `HoldSlowMotion` | HOOKED |
| `JumpToFirstMessage` | HOOKED |
| `KeepEphemeralMessages` | HOOKED |
| `KeepUnsentMessages` | HOOKED |
| `LikeUndo` | HOOKED |
| `LockDirectMessages` | HOOKED |
| `LockSavedCollections` | HOOKED |
| `LoopStories` | HOOKED |
| `MarkReadAfterReply` | HOOKED |
| `MaxUploadQuality` | HOOKED |
| `OpenLinksExternally` | HOOKED |
| `PermanentViewMode` | HOOKED |
| `PhotoZoom` | HOOKED |
| `PlaybackSpeed` | HOOKED |
| `PostDownload` | HOOKED |
| `ProfileDownload` | HOOKED |
| `ProfileFollowsYou` | HOOKED |
| `ReadReceiptChats` | HOOKED |
| `ReelDownload` | HOOKED |
| `RemoveBuildExpiredPopup` | HOOKED |
| `RemoveEmptyBottomSpace` | HOOKED |
| `RemoveMetaAI` | HOOKED |
| `ResumeReels` | HOOKED |
| `SaveInstants` | HOOKED |
| `SavedOnProfile` | HOOKED |
| `SpoofLastSeen` | SILENT |
| `SpoofLocation` | SILENT |
| `StoryDownload` | HOOKED |
| `StoryInfo` | HOOKED |
| `StoryLinks` | HOOKED |
| `StoryMentions` | HOOKED |
| `StoryViewerSearch` | HOOKED |
| `UploadInstants` | HOOKED |
| `VoiceDownload` | HOOKED |
| `airplaneMode` | HOOKED |
| `disableSwipeToCamera` | HOOKED |
| `fixNotificationRegistration` | SILENT |
| `hideOnboardingPrompts` | HOOKED |
| `hideStoriesTray` | SILENT |
| `highResolutionImages` | HOOKED |
| `reelsAutoScroll` | HOOKED |
| `reelsLockScroll` | HOOKED |
| `unlimitedAccounts` | HOOKED |

## OK — instagramUI

`instagramUI.mainActivity(hostClassLoader)` (518 ms)

Hooked (5):
- `com.instagram.base.activity.BaseFragmentActivity#onCreate(android.os.Bundle)void`
- `com.instagram.base.activity.IgFragmentActivity#onResume()void`
- `com.instagram.mainactivity.InstagramMainActivity#BCT()X.02vG`
- `android.view.View#performLongClick()boolean`
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [UIHookManager.mainActivity:184] (InstaEclipse): ✅ main onCreate hooked via com.instagram.base.activity.BaseFragmentActivity#onCreate
   [UIHookManager.mainActivity:201] (InstaEclipse): ✅ main onResume hooked via com.instagram.base.activity.IgFragmentActivity#onResume
   [BottomSheetHookUtil.hookMethod:75] (InstaEclipse | BottomSheet): ✅ Hooked: com.instagram.mainactivity.InstagramMainActivity.BCT
```

## OK — DevOptionsUnlockHook

`new DevOptionsUnlockHook().handleDevOptions(dexKitBridge)` (961 ms)

Hooked (1):
- `X.02uc#A01(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DevOptionsUnlockHook.findAndHookDynamicMethod:73] (InstaEclipse | DevOptionsEnable): 🔍 Discovery Tier 1 (String)...
   [DevOptionsUnlockHook.findAndHookDynamicMethod:105] (InstaEclipse | DevOptionsEnable): ℹ️ Tier 1 (String) found no gate on this build, trying Tier 2 (Structural)...
   [DevOptionsUnlockHook.hookExactMethod:224] (InstaEclipse | DevOptionsEnable): ✅ Hooked: X.02uc.A01
   [DevOptionsUnlockHook.findAndHookDynamicMethod:112] (InstaEclipse | DevOptionsEnable): 🎯 Found via structural match: X.02uc.A01
```

## OK — GhostDMSeenHook

`new GhostDMSeenHook().handleSeenBlock(dexKitBridge)` (162 ms)

Hooked (1):
- `X.0OCQ#A0A(com.instagram.common.session.UserSession,X.05qQ,java.lang.String,java.lang.String,java.lang.String)void`

Log:
```
   [GhostDMSeenHook.handleSeenBlock:88] (InstaEclipse | GhostModeSeen): ✅ Hooked: X.0OCQ.A0A
```

## OK — GhostDMMarkAsReadHook

`new GhostDMMarkAsReadHook(moduleSourceDir).install(lpparam.classLoader)` (52 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostChannelMarkAsReadHook

`new GhostChannelMarkAsReadHook().install(lpparam.classLoader)` (53 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostTypingIndicatorHook

`new GhostTypingIndicatorHook().handleTypingBlock(dexKitBridge)` (156 ms)

Hooked (1):
- `X.04iZ#A0R(boolean)void`

Log:
```
   [GhostTypingIndicatorHook.handleTypingBlock:86] (InstaEclipse | TypingBlock): ✅ Hooked (dynamic check): X.04iZ.A0R
```

## OK — GhostScreenshotDetectionHook

`new GhostScreenshotDetectionHook().handleScreenshotBlock(dexKitBridge)` (142 ms)

Hooked (1):
- `X.05Vs#GB9(long)void`

Log:
```
   [GhostScreenshotDetectionHook.handleScreenshotBlock:74] (InstaEclipse | ScreenshotBlock): ✅ Hooked (dynamic check): X.05Vs.GB9
```

## OK — ScreenshotPermissionHook

`new ScreenshotPermissionHook().install(lpparam.classLoader)` (53 ms)

Hooked (2):
- `android.view.Window#setFlags(int,int)void`
- `android.view.Window#addFlags(int)void`

Log:
```
   [ScreenshotPermissionHook.install:47] (InstaEclipse | ScreenshotPermission): ✅ Hooked Window.setFlags + addFlags
```

## OK — GhostViewOnceHook

`new GhostViewOnceHook().handleViewOnceBlock(dexKitBridge)` (160 ms)

Hooked (1):
- `X.05ql#H48(X.02su,X.0Mir,X.0AEc)void`

Log:
```
   [GhostViewOnceHook.tryHookViewOnce:77] (InstaEclipse | ViewOnce): ✅ Hooked (endpoint+raven_media): X.05ql.H48
```

## OK — GhostStorySeenHook

`new GhostStorySeenHook().handleStorySeenBlock(dexKitBridge)` (151 ms)

Hooked (6):
- `X.00N7#A0E(java.lang.String,java.lang.Object)void`
- `X.00N7#A0F(java.util.Map)void`
- `X.00N7#A0R(java.lang.String,java.lang.Object)void`
- `X.02nF#A0K(java.lang.Object)void`
- `X.02nF#A0S(X.02nG)void`
- `X.02nF#A0T(X.02nG)void`

Log:
```
   [GhostStorySeenHook.tryModernBatchBlock:103] (InstaEclipse | StoryBlock): ✅ Hooked 6 sender(s) on store X.02nF (item builder: X.02nG.A06)
```

## OK — KeepUnsentMessagesHook

`new KeepUnsentMessagesHook().install(dexKitBridge, lpparam.classLoader)` (726 ms)

Hooked (7):
- `androidx.fragment.app.Fragment#performResume()void`
- `androidx.fragment.app.Fragment#performPause()void`
- `androidx.fragment.app.Fragment#performDestroyView()void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#Gt1(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String)void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#Gt2(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String,boolean)void`
- `androidx.fragment.app.Fragment#onResume()void`
- `androidx.fragment.app.Fragment#onPause()void`

Log:
```
   [CurrentConversation.install:81] (IE|Conversation) lifecycle dispatchers resolved=3
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=anchors candidates=1 matches=1
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=remove-shape candidates=5 matches=1
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=remove-invokes candidates=31 matches=25
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=entry-call candidates=25 matches=1
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=ownership-call candidates=25 matches=0
   [UnsentMessageResolver.diagnostic:156] (IE|UnsentBinding-v1) stage=resolved candidates=1 matches=0
   [KeepUnsentMessagesHook.install:93] (IE|KeepUnsent) sender binding=false; optional in-chat binding=false
   [KeepUnsentMessagesHook.resolveEntryClass:395] (IE|KeepUnsent) ✅ thread entry → X.05s8
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.Gt1 (3-arg)
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.Gt2 (4-arg)
   [CurrentThreadTracker.install:95] (IE|ThreadTracker) ✅ Fragment onResume/onPause hooked
   [KeepUnsentMessagesHook.hookCurrentThread:135] (IE|KeepUnsent) current-thread tracker hooked 0 method(s)
```

## OK — UnsentThreadButtonHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.UnsentThreadButtonHook().install(lpparam.classLoader)` (58 ms)

Hooked (5):
- `android.app.Activity#onPostResume()void`
- `android.app.Activity#onPause()void`
- `android.app.Activity#onDestroy()void`
- `android.view.View#performLongClick(float,float)boolean`
- `android.view.View#performLongClick()boolean`

Log:
```
   [UnsentThreadButtonHook.install:65] (IE|UnsentBtn) ✅ installed
```

## OK — HideChatsHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.HideChatsHook().install(dexKitBridge, lpparam.classLoader)` (260 ms)

Hooked (3):
- `X.004P#<init>(android.content.Context,X.04nd,com.instagram.common.session.UserSession,com.instagram.direct.store.impl.DirectThreadStoreImpl,com.instagram.direct.store.impl.sqlite.DirectSQLiteDiskIO,X.003J,java.lang.Object)`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0c(X.009U,X.0Mmg,java.util.Comparator,java.util.List,boolean)java.util.ArrayList`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0d(java.util.List)java.util.ArrayList`

Log:
```
   [HiddenInboxFilter.installRefresh:121] (IE|HideChats) local inbox refresh method resolved
   [HiddenInboxFilter.install:93] (IE|HideChats) inbox methods resolved=2; execution requires a real inbox
```

## OK — CustomFontHook

`new io.github.somilkhan.instaeclipse2.mods.ui.CustomFontHook().install(lpparam.classLoader)` (79 ms)

Hooked (9):
- `android.graphics.Typeface#createFromAsset(android.content.res.AssetManager,java.lang.String)android.graphics.Typeface`
- `android.graphics.Typeface#createFromFile(java.io.File)android.graphics.Typeface`
- `android.graphics.Typeface#createFromFile(java.lang.String)android.graphics.Typeface`
- `android.widget.TextView#setTypeface(android.graphics.Typeface)void`
- `android.widget.TextView#setTypeface(android.graphics.Typeface,int)void`
- `android.graphics.Typeface$Builder#build()android.graphics.Typeface`
- `android.graphics.Typeface$CustomFallbackBuilder#build()android.graphics.Typeface`
- `android.app.Activity#onActivityResult(int,int,android.content.Intent)void`
- `X.00aU#run()void`

Log:
```
   [CustomFontHook.installFontHooks:55] (IE|Font) ✅ installed
   [CustomFontHook.installEmojiWorkerHook:397] (IE|Emoji) ✅ hook installed (emoji2 loader worker x1)
```

## OK — metaAi

`metaAi.install(lpparam.classLoader)` (57 ms)

Hooked (2):
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup)android.view.View`

Log:
```
   [RemoveMetaAIHook.install:126] (IE|RemoveMetaAI) ✅ installed
```

## OK — metaAi.reels

`metaAi.installReels(dexKitBridge, lpparam.classLoader)` (2227 ms)

Hooked (8):
- `X.0OVR#A01(boolean)java.util.List`
- `X.091g#A06(X.0Zoz,X.03yF,X.091g)java.util.ArrayList`
- `X.091i#A0b(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0aRA,X.0KlK)void`
- `X.091g#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0R1O)void`
- `X.091i#A08(X.0Zoz,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0aRA,X.091i)void`
- `X.091g#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0R1O)void`
- `X.091i#A0c(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0aRA,X.0KlK,java.lang.Integer,java.lang.String)void`
- `X.091i#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`

Log:
```
   [RemoveMetaAIHook.installReelsCardEligibility:340] (IE|RemoveMetaAI) reels-card eligibility: 0 method(s)
   [RemoveMetaAIHook.installVideoAttribution:376] (IE|RemoveMetaAI) video-attribution: 0 method(s)
   [RemoveMetaAIHook.installReelsOverflowGate:406] (IE|RemoveMetaAI) reels-overflow gate: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) list-GEN_AI_INFO filter: 1 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) arraylist-GEN_AI_INFO filter: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) list-GEN_AI filter: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) arraylist-GEN_AI filter: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) list-CONTENT_DEEP_DIVE filter: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) arraylist-CONTENT_DEEP_DIVE filter: 0 method(s)
   [RemoveMetaAIHook.hookBuilder:489] (IE|RemoveMetaAI) reel-options filter: 1 method(s)
   [RemoveMetaAIHook.installSearchSerpMetaAiHcm:304] (IE|RemoveMetaAI) search-serp-hcm: 0 method(s)
   [RemoveMetaAIHook.hookByMarker:271] (IE|RemoveMetaAI) cdd-should-show: 0 method(s)
   [RemoveMetaAIHook.hookByMarker:271] (IE|RemoveMetaAI) cdd-uistate: 0 method(s)
   [RemoveMetaAIHook.hookByMarker:271] (IE|RemoveMetaAI) cdd-fetch: 0 method(s)
   [RemoveMetaAIHook.hookByMarker:271] (IE|RemoveMetaAI) reel-menu-genai-row: 0 method(s)
   [RemoveMetaAIHook.installSheetRowFilter:185] (IE|RemoveMetaAI) sheet-row filter: 6 method(s)
```

## OK — DisableRepostHook

`new io.github.somilkhan.instaeclipse2.mods.ui.DisableRepostHook().install(dexKitBridge, lpparam.classLoader)` (361 ms)

Hooked (2):
- `X.0kaD#E4I(android.view.View,X.08gM,X.0Bei,java.lang.String,java.lang.String,java.lang.String,kotlin.jvm.functions.Function0,int)void`
- `X.07Gp#A0P(X.0KyQ,kotlin.jvm.functions.Function1)void`

Log:
```
   [DisableRepostHook.installFeedCommit:156] (IE|Repost) feed postRepost launcher: 1 method(s)
   [DisableRepostHook.install:73] (IE|Repost) ✅ installed: 2 method(s)
```

## OK — LockDirectMessagesHook

`new io.github.somilkhan.instaeclipse2.mods.ui.LockDirectMessagesHook().install(lpparam.classLoader)` (52 ms)

Hooked (1):
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [LockDirectMessagesHook.install:61] (IE|LockDMs) ✅ installed
```

## OK — HideSuggestedFeedItemsHook

`new HideSuggestedFeedItemsHook().install(dexKitBridge, hostClassLoader)` (595 ms)

Hooked (2):
- `X.06iy#unsafeParseFromJson(X.06e4)java.lang.Object`
- `X.06iy#parseFromJsonParser(X.06e4)java.lang.Object`

Log:
```
   [HideSuggestedFeedItemsHook.resolveHasLikedGetter:175] (InstaEclipse | HideLiked): ✅ has_liked via BuD(hash)
   [HideSuggestedFeedItemsHook.install:140] (InstaEclipse | HideSuggested): ✅ Hooked: X.06iy
```

## OK — AdBlocker

`new AdBlocker().disableSponsoredContent(dexKitBridge, hostClassLoader)` (156 ms)

Hooked (1):
- `X.06mT#A02(X.06mT,X.03xI,X.04zq)boolean`

Log:
```
   [AdBlocker.disableSponsoredContent:68] (InstaEclipse | AdBlocker): ✅ Hooked (dynamic check, marker='Is ad pod'): X.06mT.A02
```

## OK — TrackingLinkDisable

`new TrackingLinkDisable().disableTrackingLinks(hostClassLoader)` (55 ms)

Hooked (5):
- `android.content.ClipboardManager#setPrimaryClip(android.content.ClipData)void`
- `android.content.Intent#putExtra(java.lang.String,java.lang.String)android.content.Intent`
- `android.content.Intent#putExtra(java.lang.String,java.lang.CharSequence)android.content.Intent`
- `android.content.Intent#createChooser(android.content.Intent,java.lang.CharSequence)android.content.Intent`
- `android.content.Intent#createChooser(android.content.Intent,java.lang.CharSequence,android.content.IntentSender)android.content.Intent`

## OK — ExactTimestampHook

`new io.github.somilkhan.instaeclipse2.mods.misc.ExactTimestampHook().install(dexKitBridge)` (121 ms)

Hooked (1):
- `X.03gj#A08(android.content.res.Resources,X.05uG,X.03gj,java.lang.Integer,double,double,boolean,boolean,boolean,boolean)java.lang.String`

Log:
```
   [ExactTimestampHook.install:84] (InstaEclipse | ExactTime): ✅ Hooked X.03gj.A08
```

## OK — ExpandCaptionsHook

`new io.github.somilkhan.instaeclipse2.mods.feed.ExpandCaptionsHook().install(dexKitBridge)` (176 ms)

Hooked (2):
- `X.02mI#A03(android.content.Context,X.00r5,com.instagram.search.common.analytics.SearchContext,X.02mI,java.lang.String,java.lang.String,boolean)android.text.SpannableStringBuilder`
- `X.01Bb#A00(X.02mR,java.lang.CharSequence,java.lang.CharSequence,java.lang.CharSequence,int,boolean)java.lang.CharSequence`

Log:
```
   [ExpandCaptionsHook.install:67] (InstaEclipse | ExpandCaptions): ✅ Hooked X.02mI.A03 → X.01Bb.A00
```

## OK — HideCommunityNotesHook

`new io.github.somilkhan.instaeclipse2.mods.feed.HideCommunityNotesHook().install(dexKitBridge, hostClassLoader)` (63 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoMediaOverlayPayloadSchema#Cf9()X.0hiV`
- `com.instagram.api.schemas.MediaOverlayPayloadSchema#Cf9()X.0hiV`

Log:
```
   [HideCommunityNotesHook.install:71] (InstaEclipse | CommunityNotes): ✅ notes getter Cf9 hooked on 2 class(es)
```

## OK — ReelsControlsHook

`new io.github.somilkhan.instaeclipse2.mods.extras.ReelsControlsHook().install(dexKitBridge, hostClassLoader)` (770 ms)

Hooked (3):
- `X.06ST#A09(com.instagram.common.session.UserSession,boolean)boolean`
- `X.08NU#DuK(int)android.view.View`
- `instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout#onInterceptTouchEvent(android.view.MotionEvent)boolean`

Log:
```
   [ReelsControlsHook.installAutoScroll:66] (InstaEclipse | Reels): ✅ auto-scroll gate X.06ST.A09
   [ReelsControlsHook.installScrollLock:97] (InstaEclipse | Reels): ✅ pager X.08NU.A0H
```

## OK — AccountLimitHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AccountLimitHook().install(dexKitBridge, hostClassLoader)` (406 ms)

Hooked (1):
- `X.0Tx7#A01(X.06tY,int)boolean`

Log:
```
   [AccountLimitHook.install:38] (InstaEclipse | UnlimitedAccounts): ✅ hooked X.0Tx7.A01
```

## OK — AirplaneModeHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AirplaneModeHook().install(hostClassLoader)` (54 ms)

Hooked (3):
- `com.instagram.realtimeclient.RealtimeClientManager#<init>(android.content.Context,com.instagram.common.session.UserSession,com.instagram.realtimeclient.clientconfig.RealtimeClientConfig,com.instagram.realtimeclient.MainRealtimeEventHandler,X.0MaD)`
- `com.instagram.realtimeclient.RealtimeClientManager#startMqttClient()void`
- `com.instagram.realtimeclient.MainRealtimeEventHandler#onRealtimeEvent(java.lang.String,com.instagram.realtimeclient.RealtimeEvent)void`

Log:
```
   [AirplaneModeHook.install:84] (InstaEclipse | Airplane): ✅ hooked
```

## OK — HighResolutionImageHook

`new io.github.somilkhan.instaeclipse2.mods.extras.HighResolutionImageHook().install(dexKitBridge, hostClassLoader)` (178 ms)

Hooked (1):
- `X.05yQ#A02(java.lang.Integer,java.util.List,int)com.instagram.common.typedurl.ImageUrl`

Log:
```
   [HighResolutionImageHook.install:88] (InstaEclipse | HighResImages): ✅ hooked 1 picker(s)
```

## OK — OnboardingPromptHook

`new io.github.somilkhan.instaeclipse2.mods.extras.OnboardingPromptHook().install(dexKitBridge, hostClassLoader)` (143 ms)

Hooked (1):
- `X.0cdd#A06(android.content.Context,com.instagram.bloks.hosting.IgBloksScreenConfig)void`

Log:
```
   [OnboardingPromptHook.install:77] (InstaEclipse | Onboarding): ✅ hooked 1 launcher(s)
```

## NO-HOOK — NotificationRegistrationHook

`new io.github.somilkhan.instaeclipse2.mods.extras.NotificationRegistrationHook().install()` (50 ms)

## OK — StartupTabHook

`new io.github.somilkhan.instaeclipse2.mods.ui.StartupTabHook().install(hostClassLoader)` (52 ms)

Hooked (4):
- `android.app.Instrumentation#callActivityOnCreate(android.app.Activity,android.os.Bundle,android.os.PersistableBundle)void`
- `android.app.Instrumentation#callActivityOnCreate(android.app.Activity,android.os.Bundle)void`
- `android.content.Intent#getBooleanExtra(java.lang.String,boolean)boolean`
- `android.app.Instrumentation#callActivityOnResume(android.app.Activity)void`

Log:
```
   [StartupTabHook.install:113] (InstaEclipse | StartupTab): ✅ hooked Instrumentation.callActivityOnCreate x2
```

## OK — SwipeToCameraHook

`new io.github.somilkhan.instaeclipse2.mods.extras.SwipeToCameraHook().install(hostClassLoader)` (53 ms)

Hooked (1):
- `com.instagram.ui.swipenavigation.container.SwipeNavigationContainer#setInternalPosition(com.instagram.ui.swipenavigation.container.PositionConfig)void`

Log:
```
   [SwipeToCameraHook.install:69] (InstaEclipse | SwipeCamera): ✅ hooked
```

## OK — FollowingOnlyFeedHook

`new io.github.somilkhan.instaeclipse2.mods.feed.FollowingOnlyFeedHook().install(dexKitBridge, hostClassLoader)` (258 ms)

Hooked (5):
- `X.03cw#A02(android.content.Context,X.06j6,X.0UMd,com.instagram.common.session.UserSession,X.03ce,X.02vi)void`
- `X.06j6#A0L(java.lang.String,java.lang.String)void`
- `X.06j6#AB7(java.lang.String,java.lang.String)void`
- `X.06j6#ACR(java.lang.String,java.lang.String)void`
- `X.06j6#ACj(java.lang.String,java.lang.String)void`

Log:
```
   [FollowingOnlyFeedHook.install:129] (InstaEclipse | FollowingFeed): ✅ hooked X.03cw.A02 + 4 adders
```

## OK — ProfileFollowsYouHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ProfileFollowsYouHook().install(dexKitBridge, hostClassLoader)` (152 ms)

Hooked (2):
- `com.instagram.profile.fragment.UserDetailFragment#A0n(com.instagram.profile.fragment.UserDetailFragment,boolean)void`
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [IgModels.ensure:104] (IE|Models) ✅ User model = X.02fy
   [ProfileFollowsYouHook.install:102] (InstaEclipse | FollowsYou): ✅ Hooked A0n / followed_by via hashed accessor
```

## OK — PlaybackSpeedHook

`new io.github.somilkhan.instaeclipse2.mods.media.PlaybackSpeedHook().install(dexKitBridge, hostClassLoader)` (2162 ms)

Hooked (25):
- `X.03Zk#A0P(com.instagram.feed.media.Media,X.0Jby,X.0JAB,X.08gM,X.0JAZ,int,boolean)boolean`
- `X.03Bb#GZs(long,long)void`
- `X.0ArJ#HPS(float)void`
- `X.0ArJ#<init>(X.06o3,com.instagram.common.session.UserSession,X.00y4,X.0ArR,java.lang.String)`
- `X.0CeU#HPS(float)void`
- `X.0CeU#<init>(android.content.Context,com.instagram.common.session.UserSession,X.07qF,X.00y4,X.0VAC)`
- `X.0CfT#HPS(float)void`
- `X.0CfT#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0Jby,X.0Tgl,X.0CfS,kotlin.jvm.functions.Function1,boolean,boolean)`
- `X.0CmS#HPS(float)void`
- `X.0CmS#<init>(android.content.Context,com.instagram.common.session.UserSession,X.07qF,X.00y4,X.0VAC)`
- `X.0T2V#HPS(float)void`
- `X.0T2V#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0Jby,java.lang.String)`
- `X.0V8r#HPS(float)void`
- `X.03Fa#A0D(X.03Fa,float,float)void`
- `X.03Fa#A02(X.03Fa)void`
- `X.0JBF#A00(X.0JBF)java.lang.Object`
- `android.app.Activity#dispatchTouchEvent(android.view.MotionEvent)boolean`
- `android.widget.TextView#setText(java.lang.CharSequence,android.widget.TextView$BufferType)void`
- `android.text.StaticLayout$Builder#obtain(java.lang.CharSequence,int,int,android.text.TextPaint,int)android.text.StaticLayout$Builder`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int)android.text.BoringLayout`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int)android.text.BoringLayout`

Log:
```
   [PlaybackSpeedHook.installFeed:511] (InstaEclipse | PlaybackSpeed): story/reel speed methods: 6
   [PlaybackSpeedHook.installFeed:512] (InstaEclipse | PlaybackSpeed): ✅ feed player X.03Zk.A0P, started-cb X.03Bb.GZs, setters 1
   [PlaybackSpeedHook.installHero:268] (InstaEclipse | PlaybackSpeed): ✅ hero X.03Fa.A0D, prepare A02
   [PlaybackSpeedHook.installScrubberFlags:703] (InstaEclipse | PlaybackSpeed): ✅ reel scrubber flags X.083g{A0B,A0C,A0F,A0G} via X.0JBF.A00
   [PlaybackSpeedHook.installHoldSide:153] (InstaEclipse | PlaybackSpeed): ✅ hold side tracking
```

## OK — HideNotesTrayHook

`new io.github.somilkhan.instaeclipse2.mods.ui.HideNotesTrayHook().install(dexKitBridge)` (184 ms)

Hooked (1):
- `X.0MG1#invoke(java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [HideNotesTrayHook.installTaggedTray:112] (InstaEclipse | NotesTray): ✅ tagged tray X.0MG1.invoke
```

## OK — FeedUfiButtonsHook

`new io.github.somilkhan.instaeclipse2.mods.feed.FeedUfiButtonsHook().install(dexKitBridge)` (68 ms)

Hooked (1):
- `X.04d9#<init>(X.00P6,com.instagram.common.typedurl.ImageUrl,com.instagram.feed.media.Media,X.08gM,X.04SJ,X.0j0m,X.04d8,X.01Jf,X.01Ij,X.01w3,X.01j4,java.lang.Integer,java.lang.String,int,int,int,int,int,long,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean)`

Log:
```
   [FeedUfiButtonsHook.install:84] (InstaEclipse | FeedUfi): ✅ Hooked 1 constructor(s) of X.04d9
```

## OK — ProfileHighlightsHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ProfileHighlightsHook().install(dexKitBridge)` (65 ms)

Hooked (1):
- `X.0JEo#bindView(int,android.view.View,java.lang.Object,java.lang.Object)void`

Log:
```
   [ProfileHighlightsHook.install:69] (InstaEclipse | Highlights): ✅ Hooked X.0JEo.bindView
```

## OK — HideHiddenRequestsHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.HideHiddenRequestsHook().install()` (52 ms)

Hooked (1):
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`

Log:
```
   [HideHiddenRequestsHook.install:45] (InstaEclipse | HiddenRequests): ✅ Hooked LayoutInflater.inflate
```

## OK — BottomSpaceHook

`new io.github.somilkhan.instaeclipse2.mods.ui.BottomSpaceHook().install(dexKitBridge)` (154 ms)

Hooked (1):
- `X.08be#A00(android.app.Activity,int,int,int)int`

Log:
```
   [BottomSpaceHook.install:57] (InstaEclipse | BottomSpace): ✅ Hooked X.08be.A00
```

## OK — FollowButtonsHook.stories

`new io.github.somilkhan.instaeclipse2.mods.piko.FollowButtonsHook().installStories(dexKitBridge)` (248 ms)

Hooked (1):
- `X.0KbR#A0R(java.lang.Object)void`

Log:
```
   [FollowButtonsHook.installStories:97] (InstaEclipse | FollowButtons): ✅ Hooked story component bind X.0KbR.A0R
```

## OK — FollowButtonsHook.reels

`new io.github.somilkhan.instaeclipse2.mods.piko.FollowButtonsHook().installReels()` (52 ms)

Hooked (3):
- `com.instagram.user.follow.FollowButtonBase#<init>(android.content.Context,android.util.AttributeSet)`
- `com.instagram.user.follow.FollowButtonBase#<init>(android.content.Context)`
- `com.instagram.user.follow.FollowButtonBase#<init>(android.content.Context,android.util.AttributeSet,int)`

Log:
```
   [FollowButtonsHook.installReels:197] (InstaEclipse | FollowButtons): ✅ Hooked 3 FollowButtonBase constructors
```

## OK — LoopStoryHook

`new io.github.somilkhan.instaeclipse2.mods.piko.LoopStoryHook().install(dexKitBridge)` (121 ms)

Hooked (2):
- `instagram.features.stories.fragment.ReelViewerFragment#A1U(com.instagram.model.reels.ReelItem)void`
- `instagram.features.stories.fragment.ReelViewerFragment#A1G(com.instagram.model.reels.ReelItem)boolean`

Log:
```
   [LoopStoryHook.install:125] (InstaEclipse | LoopStory): ✅ Hooked A1U / A1G (video seek H3P)
```

## OK — DisappearingSwipeHook

`new io.github.somilkhan.instaeclipse2.mods.piko.DisappearingSwipeHook().install(dexKitBridge)` (483 ms)

Hooked (2):
- `X.07Lw#A01()void`
- `X.02PA#A07(com.instagram.common.session.UserSession,X.04Wr,boolean)boolean`

Log:
```
   [DisappearingSwipeHook.install:112] (InstaEclipse | DisappearingSwipe): ✅ Hooked X.07Lw.A01 gate X.02PA.A07
```

## OK — ChatCallButtonsHook

`new io.github.somilkhan.instaeclipse2.mods.piko.ChatCallButtonsHook().install(dexKitBridge)` (127 ms)

Hooked (1):
- `X.04ZL#A06(android.content.Context,X.06Hu,X.04ZL)java.util.ArrayList`

Log:
```
   [ChatCallButtonsHook.install:88] (InstaEclipse | ChatCallButtons): ✅ Hooked X.04ZL.A06
```

## OK — StoryTrayFilterHook

`new io.github.somilkhan.instaeclipse2.mods.piko.StoryTrayFilterHook().install(dexKitBridge)` (214 ms)

Hooked (1):
- `X.04nc#A0H(X.07Hd,X.02fy,X.04ng,X.04nc,java.lang.String,java.util.List,java.util.List,boolean,boolean)void`

Log:
```
   [StoryTrayFilterHook.install:91] (InstaEclipse | StoryFilter): ✅ Hooked X.04nc.A0H (type getter pando, user getter DqA)
```

## OK — LikeAnimationHook

`new io.github.somilkhan.instaeclipse2.mods.piko.LikeAnimationHook().install()` (55 ms)

Hooked (2):
- `com.instagram.api.schemas.XDTUserActivationMetadataImpl#<init>(X.08eU,java.lang.Boolean,java.lang.Boolean,java.lang.Boolean,java.lang.Boolean,java.lang.Boolean)`
- `com.instagram.ui.mediaactions.LikeActionView#A00(com.instagram.common.session.UserSession,X.052j)void`

Log:
```
   [LikeAnimationHook.installMetadata:136] (InstaEclipse | LikeAnimation): ✅ Hooked activation metadata (1 ctor, 26 animations)
   [LikeAnimationHook.installView:246] (InstaEclipse | LikeAnimation): ✅ Hooked LikeActionView.A00 (26 animations, mapper found)
```

## OK — StoryRingSizeHook

`new io.github.somilkhan.instaeclipse2.mods.piko.StoryRingSizeHook().install(dexKitBridge)` (71 ms)

Hooked (2):
- `X.07iK#<init>(android.content.Context,com.instagram.common.session.UserSession,java.lang.Integer,float,int,int,int,int,boolean)`
- `X.07td#A06(android.content.Context,float)float`

Log:
```
   [StoryRingSizeHook.install:102] (InstaEclipse | StoryRingSize): ✅ Hooked X.07iK.<init> via X.07td.A06 = 100dp
```

## OK — ResumeReelsHook

`new io.github.somilkhan.instaeclipse2.mods.piko.ResumeReelsHook().install(dexKitBridge)` (158 ms)

Hooked (2):
- `X.0ArJ#GfP(java.lang.String)int`
- `X.0ArJ#Gj1(X.06kT,X.03yF,X.09IE,X.03BM,X.0Pag,float,int,int,boolean,boolean,boolean,boolean)boolean`

Log:
```
   [ResumeReelsHook.install:108] (InstaEclipse | ResumeReels): ✅ Hooked X.0ArJ.GfP / Gj1
```

## OK — MarkReadAfterReplyHook

`new io.github.somilkhan.instaeclipse2.mods.piko.MarkReadAfterReplyHook().install(dexKitBridge)` (259 ms)

Hooked (1):
- `X.07Yv#Fpr(X.02su,X.0AEc)void`

Log:
```
   [MarkReadAfterReplyHook.install:84] (InstaEclipse | MarkReadAfterReply): ✅ Hooked X.07Yv.Fpr for X.0GFs
```

## PARTIAL — VoiceMessageDownloadHook

`new io.github.somilkhan.instaeclipse2.mods.media.VoiceMessageDownloadHook().install(dexKitBridge, hostClassLoader)` (573 ms)

Hooked (5):
- `com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel#A00(android.content.Context,android.graphics.PointF,android.graphics.Rect,com.instagram.aistudio.logging.AiStudioLoggingData,com.instagram.common.session.UserSession,X.03Yp,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,java.util.List,java.util.List,int,int,long,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean)com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel`
- `X.0ale#A08(android.app.Activity,android.view.View,com.instagram.common.session.UserSession,X.0hiy,X.0MaQ,X.0gzP,X.0hdm,X.0gzs,X.0Mjo,X.0Mfo,X.0hft,X.0hAg,X.0Mac,X.0Mae,X.0hAu,X.0Mik,X.0hB4,X.0hB7,X.0hB8,X.0hbD,X.0hB9,X.0hBB,X.0hBC,X.0hBD,X.0gmX,X.0Mil,X.0hBF,X.0Mjq,X.0Min,X.0hBL,X.0Mgc,X.0Mbm,X.0hdo,X.0TrD,X.0Gpm,X.05oJ,com.instagram.model.direct.DirectThreadKey,X.03Yp,com.instagram.model.direct.messageid.MessageIdentifier,X.0ZAY,X.05Gn,X.05Gn,X.05Gn,X.05Gn,X.05Gn,X.0ZAp,X.0KoA,X.0KoA,X.0KoA,X.0KoA,X.0KoA,X.0KoA,X.0Hyk,X.05GZ,X.05GZ,X.0ZOe,java.lang.String,java.lang.String,java.lang.String,java.util.List,kotlin.jvm.functions.Function0,int,long)void`
- `X.0bLw#A02(com.instagram.direct.messagethread.interaction.longpressaction.LongPressActionData,X.0bLw,java.lang.String)void`
- `X.0Kom#Dzw(X.0Gpm,java.lang.String)void`
- `X.0cAU#Dzw(X.0Gpm,java.lang.String)void`

Log:
```
!! [VoiceMessageDownloadHook.install:383] (IE|VoiceDL) ⚠️ compose rows: java.lang.reflect.InvocationTargetException / java.lang.UnsupportedOperationException: hook-verify stub: Instagram code does not run on the host JVM
!! [VoiceMessageDownloadHook.install:385] (IE|VoiceDL) compose row hooks=0 (label field not found)
   [VoiceMessageDownloadHook.install:387] (IE|VoiceDL) ✅ lookups=0 factories=1 dispatchers=4 mapper=true message=X.05oJ
!! [VoiceMessageDownloadHook.install:397] (IE|VoiceDL) ⚠️ translate row unavailable: factories=1 labelField=false
   [BulkUnsend.install:142] (IE|BulkUnsend) ✅ unsend=X.05Hn.A0I ref=X.0Fik
```

## OK — EditHistoryHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.EditHistoryHook().install(dexKitBridge, hostClassLoader)` (220 ms)

Hooked (2):
- `X.05oJ#A18(com.instagram.common.session.UserSession,X.05oJ,X.03ZY)void`
- `X.05oJ#A10()java.util.List`

Log:
```
   [EditHistoryHook.install:149] (IE|EditHistory) ✅ merge A18, history getter A10
```

## NO-HOOK — JumpToFirstMessageHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.JumpToFirstMessageHook().install()` (52 ms)

Log:
```
   [JumpToFirstMessageHook.install:51] (IE|JumpFirst) ✅ installed
```

## NO-HOOK — ReadReceiptChatHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.ReadReceiptChatHook().install(hostClassLoader)` (52 ms)

Log:
```
   [ReadReceiptChatHook.install:51] (IE|ReadReceiptChat) ✅ installed
```

## OK — StoryViewerSearchHook

`new io.github.somilkhan.instaeclipse2.mods.ui.StoryViewerSearchHook().install(dexKitBridge, hostClassLoader)` (262 ms)

Hooked (1):
- `X.0SvC#A07(java.util.List,boolean,boolean,boolean)void`

Log:
```
   [StoryViewerSearchHook.install:105] (IE|ViewerSearch) ✅ adapter X.0SvC.A07 rebuild=none
```

## OK — CommentSearchHook

`new io.github.somilkhan.instaeclipse2.mods.misc.CommentSearchHook().install(dexKitBridge, hostClassLoader)` (430 ms)

Hooked (2):
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#A07(X.022I,X.022H,X.022H,X.022Z,X.024t,X.025w,X.026C,X.020r,X.022B,X.025p,com.instagram.comments.mvvm.viewmodel.CommentListViewModel,X.0Ou5,X.0TtD,X.0PIi,boolean,boolean,boolean)X.0imt`
- `X.0QKP#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [CommentSearchHook.install:131] (IE|CommentSearch) comment json: serializer=true factory=true
   [CommentSearchHook.install:152] (IE|CommentSearch) ✅ vm com.instagram.comments.mvvm.viewmodel.CommentListViewModel builders=1 fragments=1
```

## OK — ChatExportHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.ChatExportHook().install(dexKitBridge, hostClassLoader)` (312 ms)

Hooked (1):
- `X.05s8#<init>(com.instagram.common.session.UserSession,X.05qG,java.util.List)`

Log:
```
   [ChatExportHook.install:125] (IE|ChatExport) ✅ store X.05s8 messages=true serializer=true factory=X.02co
```

## OK — FollowListExportHook

`new io.github.somilkhan.instaeclipse2.mods.misc.FollowListExportHook().install(dexKitBridge, hostClassLoader)` (312 ms)

Hooked (1):
- `X.0QdK#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [FollowListExportHook.findPager:233] (IE|FollowExport) pager=X.0BTW loadMore=AKu
   [FollowListExportHook.install:107] (IE|FollowExport) ✅ X.0QdK fullName=false
```

## OK — LikeUndoHook

`new io.github.somilkhan.instaeclipse2.mods.misc.LikeUndoHook().install(dexKitBridge)` (1118 ms)

Hooked (1):
- `X.0TfL#A03(android.content.Context,X.08Bg,com.instagram.common.session.UserSession,X.0Zag,com.instagram.feed.media.Media,X.00f6,com.instagram.search.common.analytics.SearchContext,java.lang.Double,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.Map,boolean)void`

Log:
```
   [LikeUndoHook.install:130] (InstaEclipse | LikeUndo): ✅ Hooked X.0TfL.A03
```

## OK — ShareSheetCleanupHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ShareSheetCleanupHook().install(hostClassLoader)` (55 ms)

Hooked (2):
- `instagram.features.direct.fragment.sharesheet.DirectShareSheetFragment#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`

Log:
```
   [ShareSheetCleanupHook.install:99] (IE|ShareSheet) ✅ installed (2 hooks)
```

## NO-HOOK — ExternalBrowserHook

`new io.github.somilkhan.instaeclipse2.mods.misc.ExternalBrowserHook().install()` (51 ms)

Log:
```
   [ExternalBrowserHook.install:44] (IE|ExtBrowser) ✅ hooked execStartActivity x0
```

## OK — DisableStoryFlippingHook

`new DisableStoryFlippingHook().handleStoryFlippingDisable(dexKitBridge)` (83 ms)

Hooked (1):
- `instagram.features.stories.fragment.ReelViewerFragment#A1U(com.instagram.model.reels.ReelItem)void`

Log:
```
   [DisableStoryFlippingHook.findAndHookMethod:104] (InstaEclipse | StoryFlipping): ✅ Hooked (dynamic check): instagram.features.stories.fragment.ReelViewerFragment.A1U
```

## OK — StoryMentionHook

`new StoryMentionHook().install(dexKitBridge, lpparam.classLoader)` (501 ms)

Hooked (11):
- `X.0iNl#A0r(X.07qF,X.0iNl,X.0MZg,boolean)java.lang.CharSequence[]`
- `X.0iNl#A0s(X.0iNl)java.lang.CharSequence[]`
- `X.0iNl#A0t(X.0iNl,boolean)java.lang.CharSequence[]`
- `X.0hFY#A01(android.content.Context,X.0hFY)void`
- `X.0hFY#A03(X.0hFY,java.lang.CharSequence)void`
- `X.0iNl#A0F(android.content.Context,X.0aRA,com.instagram.model.reels.ReelItem,X.0iNl,X.0FSM)void`
- `X.0iNl#A0G(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0mjn,X.0mjo,X.0Uit,X.0mjs,X.0mjw,X.0iNl,X.0FSk,X.0FRP,X.0FVn,X.0FSO,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0I(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0Uit,X.0mjw,X.0iNl,X.0FTP,X.0FRP,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0J(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Uit,X.0mqm,X.0mju,X.0iNl,X.0FVn,X.0FUP,X.0FSM,X.0FVP,java.lang.CharSequence)void`
- `X.0iNl#A0d(java.util.List)void`
- `X.0iNl#A0u(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Bg,X.0hlw,X.0Uit,X.0mqm,X.0mju,X.0mjv,X.0FVn,X.0FUP,X.0FSM,X.0FVP,X.0FVO)void`

Log:
```
   [StoryMentionHook.resolveMentionPipeline:119] (IE|Mention) ✅ reel_mentions via hashed accessor
   [StoryMentionHook.resolveMentionPipeline:165] (IE|Mention) ✅ pipeline resolved: hash(reel_mentions) -> A0E
   [StoryMentionHook.installButtonHook:245] (IE|Mention) ✅ button hook installed on 3 builder(s)
   [StoryMentionHook.installClickHook:300] (IE|Mention) ✅ click hook installed
```

## OK — StoryLinksHook

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryLinksHook().install(dexKitBridge, lpparam.classLoader)` (330 ms)

Hooked (11):
- `X.0iNl#A0r(X.07qF,X.0iNl,X.0MZg,boolean)java.lang.CharSequence[]`
- `X.0iNl#A0s(X.0iNl)java.lang.CharSequence[]`
- `X.0iNl#A0t(X.0iNl,boolean)java.lang.CharSequence[]`
- `X.0hFY#A01(android.content.Context,X.0hFY)void`
- `X.0hFY#A03(X.0hFY,java.lang.CharSequence)void`
- `X.0iNl#A0F(android.content.Context,X.0aRA,com.instagram.model.reels.ReelItem,X.0iNl,X.0FSM)void`
- `X.0iNl#A0G(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0mjn,X.0mjo,X.0Uit,X.0mjs,X.0mjw,X.0iNl,X.0FSk,X.0FRP,X.0FVn,X.0FSO,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0I(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0Uit,X.0mjw,X.0iNl,X.0FTP,X.0FRP,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0J(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Uit,X.0mqm,X.0mju,X.0iNl,X.0FVn,X.0FUP,X.0FSM,X.0FVP,java.lang.CharSequence)void`
- `X.0iNl#A0d(java.util.List)void`
- `X.0iNl#A0u(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Bg,X.0hlw,X.0Uit,X.0mqm,X.0mju,X.0mjv,X.0FVn,X.0FUP,X.0FSM,X.0FVP,X.0FVO)void`

Log:
```
   [StoryLinksHook.install:70] (IE|StoryLinks) ✅ link stickers via hashed accessor
   [StoryLinksHook.installButtonHook:155] (IE|StoryLinks) ✅ button hook on 3 builder(s)
   [StoryLinksHook.installClickHook:201] (IE|StoryLinks) ✅ click hook installed
```

## OK — StoryInfoHook

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryInfoHook().install(dexKitBridge, lpparam.classLoader)` (1504 ms)

Hooked (11):
- `X.0iNl#A0r(X.07qF,X.0iNl,X.0MZg,boolean)java.lang.CharSequence[]`
- `X.0iNl#A0s(X.0iNl)java.lang.CharSequence[]`
- `X.0iNl#A0t(X.0iNl,boolean)java.lang.CharSequence[]`
- `X.0hFY#A01(android.content.Context,X.0hFY)void`
- `X.0hFY#A03(X.0hFY,java.lang.CharSequence)void`
- `X.0iNl#A0F(android.content.Context,X.0aRA,com.instagram.model.reels.ReelItem,X.0iNl,X.0FSM)void`
- `X.0iNl#A0G(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0mjn,X.0mjo,X.0Uit,X.0mjs,X.0mjw,X.0iNl,X.0FSk,X.0FRP,X.0FVn,X.0FSO,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0I(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0Uit,X.0mjw,X.0iNl,X.0FTP,X.0FRP,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0J(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Uit,X.0mqm,X.0mju,X.0iNl,X.0FVn,X.0FUP,X.0FSM,X.0FVP,java.lang.CharSequence)void`
- `X.0iNl#A0d(java.util.List)void`
- `X.0iNl#A0u(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Bg,X.0hlw,X.0Uit,X.0mqm,X.0mju,X.0mjv,X.0FVn,X.0FUP,X.0FSM,X.0FVP,X.0FVO)void`

Log:
```
   [StoryInfoHook.install:87] (IE|StoryInfo) ✅ Media fields via hashed accessor
   [StoryInfoHook.installButtonHook:187] (IE|StoryInfo) ✅ button hook on 3 builder(s)
   [StoryInfoHook.installClickHook:240] (IE|StoryInfo) ✅ click hook installed
```

## OK — CommentCopyHook

`new CommentCopyHook().install(dexKitBridge, lpparam.classLoader)` (207 ms)

Hooked (4):
- `X.0QKP#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#GFn(java.lang.String,java.lang.String,float,boolean)void`

Log:
```
   [CommentCopyHook.findAndHookNew:200] (InstaEclipse | CopyComment): ✅ Hooked com.instagram.comments.mvvm.viewmodel.CommentListViewModel.GFn (resolver=X.027t.A01)
```

## OK — CaptionCopyContextMenuHook

`new CaptionCopyContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (1151 ms)

Hooked (11):
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `X.0PP1#A00(X.05yz,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PP1,X.0ETt,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.091i#A0f(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.091i#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Py9#A0A(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0OVR#A01(boolean)java.util.List`
- `X.091g#A06(X.0Zoz,X.03yF,X.091g)java.util.ArrayList`
- `X.091k#A01(com.instagram.common.session.UserSession,com.instagram.feed.media.mediaoption.MediaOption$Option,boolean)int`
- `X.0aRA#A00(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,X.0Zeb,X.0k3j,java.lang.Integer,java.lang.String,java.lang.String,java.lang.String,float,boolean)void`
- `X.0aRA#A01(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,java.lang.String,java.lang.String,float,int,int,boolean,boolean,boolean,boolean,boolean)void`

Log:
```
   [CaptionCopyContextMenuHook.resolveCaptionGetter:233] (IE|Caption) ✅ caption via hashed accessor
   [CaptionCopyContextMenuHook.installAddButtonHook:525] (IE|Caption) ✅ Caption copy menu hook installed
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:793] (IE|Caption) ✅ reel label override on X.0aRA.A00
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:793] (IE|Caption) ✅ reel label override on X.0aRA.A01
```

## OK — DisableDoubleTapLikeHook

`new DisableDoubleTapLikeHook().install(dexKitBridge, lpparam.classLoader)` (770 ms)

Hooked (2):
- `X.0TfL#A02(android.app.Activity,android.content.Context,X.03uZ,com.instagram.common.session.UserSession,X.0Zag,com.instagram.feed.media.Media,X.00f6,X.08Ci,X.0Jby,X.0mry,com.instagram.search.common.analytics.SearchContext,X.0Ywn,java.lang.Double,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,int,int,int,boolean)void`
- `X.0JhK#invoke(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [DisableDoubleTapLikeHook.findAndHook:139] (InstaEclipse | DoubleTapLike): Feed hooked on X.0TfL.A02
   [DisableDoubleTapLikeHook.findAndHook:161] (InstaEclipse | DoubleTapLike): Reels like action hooked on X.0JhK.invoke
```

## OK — DoubleTapExtrasHook

`new io.github.somilkhan.instaeclipse2.mods.misc.DoubleTapExtrasHook().install(dexKitBridge, lpparam.classLoader)` (304 ms)

Hooked (6):
- `X.02O7#onDoubleTap(android.view.MotionEvent)boolean`
- `X.02R9#onDoubleTap(android.view.MotionEvent)boolean`
- `X.0XiW#FMD()void`
- `X.0bv0#FnA(X.03Yp,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.0bwN#FnA(X.03Yp,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.09v2#A00(X.0Mae,X.03Yp,X.02pA,java.lang.String,java.lang.String,java.util.List,boolean)void`

Log:
```
   [DoubleTapExtrasHook.install:101] (InstaEclipse | DoubleTapLike): ✅ comments 2, DM senders 3, DM double-tap 1
```

## OK — FeedPhotoZoomHook

`new FeedPhotoZoomHook().install(lpparam.classLoader)` (54 ms)

Hooked (1):
- `com.instagram.common.ui.widget.imageview.IgProgressImageView#onAttachedToWindow()void`

Log:
```
   [FeedPhotoZoomHook.install:100] (IE|PhotoZoom) ✅ hook installed on com.instagram.common.ui.widget.imageview.IgProgressImageView
```

## FAIL — LocationSpoofHook

`new LocationSpoofHook().install(lpparam.classLoader)` (52 ms)

Log:
```
!! [LocationSpoofHook.install:56] (InstaEclipse | SpoofLocation): ❌ Install failed: Cannot invoke "Object.getClass()" because "android.location.Location.CREATOR" is null
```

## OK — IgThemeHook

`new IgThemeHook().install(hostClassLoader)` (66 ms)

Hooked (21):
- `android.content.res.Resources$Theme#resolveAttribute(int,android.util.TypedValue,boolean)boolean`
- `android.content.res.Resources#getColor(int,android.content.res.Resources$Theme)int`
- `android.content.res.Resources#getColor(int)int`
- `android.content.Context#getColor(int)int`
- `android.content.res.TypedArray#getColor(int,int)int`
- `androidx.compose.ui.platform.AndroidComposeView#dispatchDraw(android.graphics.Canvas)void`
- `android.graphics.Paint#setColor(int)void`
- `android.content.res.TypedArray#getColorStateList(int)android.content.res.ColorStateList`
- `android.content.res.Resources#getColorStateList(int,android.content.res.Resources$Theme)android.content.res.ColorStateList`
- `android.content.res.Resources#getColorStateList(int)android.content.res.ColorStateList`
- `android.content.res.ColorStateList#valueOf(int)android.content.res.ColorStateList`
- `android.graphics.drawable.ColorDrawable#setColor(int)void`
- `android.graphics.drawable.ColorDrawable#<init>(int)`
- `android.graphics.drawable.ColorDrawable#<init>(int)`
- `android.graphics.drawable.ColorDrawable#<init>()`
- `android.graphics.drawable.ColorDrawable#draw(android.graphics.Canvas)void`
- `android.graphics.drawable.GradientDrawable#setColor(int)void`
- `android.graphics.drawable.GradientDrawable#setColor(android.content.res.ColorStateList)void`
- `android.graphics.PorterDuffColorFilter#<init>(int,android.graphics.PorterDuff$Mode)`
- `android.app.Activity#onResume()void`
- `android.app.Activity#onCreate(android.os.Bundle)void`

Log:
```
   [IgThemeColorSourceHooks.install:57] (InstaEclipse | Theme): color-source hooks installed=12
   [IgThemeHook.install:62] (InstaEclipse | Theme): hooks installed enabled=true
```

## OK — ForceReelQualityHook

`new ForceReelQualityHook().install(dexKitBridge, lpparam.classLoader)` (107 ms)

Hooked (11):
- `com.instagram.feed.media.Media#BO2(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#Cim(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#Cio(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#Cip(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#Ciq(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#Cir(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#D83(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#D84(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#D86(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#D88(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#D89(int)com.google.common.collect.ImmutableList`

Log:
```
   [ForceReelQualityHook.installHashed:198] (InstaEclipse | ForceReelQuality): ✅ Hooked com.instagram.feed.media.Media list accessors x11 [video_versions hash] (height=C67)
```

## OK — MaxUploadQualityHook

`new io.github.somilkhan.instaeclipse2.mods.media.MaxUploadQualityHook().install(dexKitBridge, lpparam.classLoader)` (410 ms)

Hooked (1):
- `X.0BOA#A00(X.02pA)boolean`

Log:
```
   [MaxUploadQualityHook.install:73] (IE|MaxUploadQuality) ✅ hooked 1 method(s)
```

## OK — DisableVideoAutoPlayHook

`new DisableVideoAutoPlayHook().handleAutoPlayDisable(dexKitBridge)` (169 ms)

Hooked (9):
- `android.content.res.Resources#getDrawable(int)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawable(int,android.content.res.Resources$Theme)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawableForDensity(int,int,android.content.res.Resources$Theme)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawableForDensity(int,int)android.graphics.drawable.Drawable`
- `android.widget.ImageView#setImageResource(int)void`
- `android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)void`
- `android.view.View#performClick()boolean`
- `android.view.View#callOnClick()boolean`
- `X.00vV#A00(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DisableVideoAutoPlayHook.hookManualPlayOverlayLifecycle:121] (InstaEclipse | AutoPlayDisable): Hooked manual play overlay lifecycle.
   [DisableVideoAutoPlayHook.hookMethod:94] (InstaEclipse | AutoPlayDisable): ✅ Hooked (dynamic check): X.00vV.A00
```

## OK — BuildExpiredPopupHook

`new BuildExpiredPopupHook().install(dexKitBridge, lpparam.classLoader)` (273 ms)

Hooked (2):
- `X.03AW#A02(androidx.fragment.app.FragmentActivity,X.0nda)void`
- `X.03AW#A05()boolean`

Log:
```
   [BuildExpiredPopupHook.install:86] (IE|BuildExpired) ✅ hooked show-popup → X.03AW.A02
   [BuildExpiredPopupHook.install:109] (IE|BuildExpired) ✅ hooked snooze-check → X.03AW.A05
```

## NO-HOOK — FeedVideoDownloadHook

`new FeedVideoDownloadHook().install(lpparam.classLoader)` (55 ms)

Log:
```
   [FeedVideoDownloadHook.install:191] (IE|DL) media model: mutable=false liveTree=false listCandidates=0
```

## OK — FeedVideoDownloadHook.videoUrlCapture

`FeedVideoDownloadHook.installVideoUrlCaptureHook(dexKitBridge, lpparam.classLoader)` (2252 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoVideoVersion#getUrl()java.lang.String`
- `com.instagram.api.schemas.VideoVersion#getUrl()java.lang.String`

Log:
```
   [SelectedMediaAdapter.install:63] (IE|MediaAdapter) resolved properties=5 manifest=false direct=false story=true hashed=true
   [FeedVideoDownloadHook.discoverDynamicMediaModel:1310] (IE|DL|DexKit) dynamic media dict=X.06rh
   [FeedVideoDownloadHook.resolveVideoVersionsGetters:1366] (IE|DL|DexKit) video_versions getters=0
   [FeedVideoDownloadHook.resolveCarouselGetter:1411] (IE|DL|DexKit) carousel getter=hash(carousel_media)
   [FeedVideoDownloadHook.resolveDashManifestGetter:1484] (IE|DL|DexKit) dash manifest getter=hash(video_dash_manifest)
   [FeedVideoDownloadHook.resolveHasAudioGetter:1438] (IE|DL|DexKit) has_audio getter=hash(has_audio)
   [FeedVideoDownloadHook.resolveIsVideoMethod:1858] (IE|DL|DexKit) isVideo: no Media ()Z getter (458+ layout), using hash(video_versions) presence
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1236] (IE|DL|DexKit) VideoVersionIntf implementors found: 2
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1252] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.ImmutablePandoVideoVersion
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1252] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.VideoVersion
   [FeedVideoDownloadHook.resolveUsernameGetter:1905] (IE|DL|Username) userClass=X.02fy
   [FeedVideoDownloadHook.resolveUsernameGetter:1950] (IE|DL|Username) ✅ username via hashed accessor on X.02fy
   [FeedVideoDownloadHook.resolveMediaAuthorGetter:1996] (IE|DL|Username) ✅ media author via hash(user)
   [FeedVideoDownloadHook.resolveDictUserGetter:2074] (IE|DL|Username) dictUserGetter: not needed (458+ layout), using hash(user)
```

## OK — PostDownloadContextMenuHook

`new PostDownloadContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (361 ms)

Hooked (5):
- `X.0PP1#A00(X.05yz,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PP1,X.0ETt,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.091i#A0f(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.091i#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Py9#A0A(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0OVR#A01(boolean)java.util.List`

Log:
```
   [PostDownloadContextMenuHook.installAddButtonHook:354] (IE|Post) ✅ Post menu inject hook installed
   [PostDownloadContextMenuHook.resolveReelClickDispatcher:397] (IE|Post) selected Reel dispatcher bound=false
   [PostDownloadContextMenuHook.installAllowlistPatchHook:552] (IE|Post) ✅ Allowlist patch hooked: X.0OVR.A01
```

## OK — InstantSaveHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantSaveHook().install(lpparam.classLoader)` (54 ms)

Hooked (2):
- `com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1#<init>(androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,X.0F65,com.instagram.common.session.UserSession,com.instagram.compose.ui.emitter.AnimatedParticleEffectState,X.02PY,java.util.List,java.util.List,X.0Mho,int,boolean)`
- `com.instagram.quicksnap.viewer.compose.PointerTouchKt$handleTapAndLongPressWithRelease$2$1$1$1#<init>(X.0Mho,kotlin.jvm.functions.Function0,kotlin.jvm.functions.Function1,kotlin.jvm.functions.Function1,float,long,boolean)`

Log:
```
   [InstantSaveHook.cacheCurrentInstant:77] (IE|InstantSave) ✅ viewer cache hooked: com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1
   [InstantSaveHook.wrapLongPress:183] (IE|InstantSave) ✅ long-press hooked
```

## OK — InstantUploadHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantUploadHook().install(lpparam.classLoader)` (55 ms)

Hooked (10):
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1B(android.content.Context,android.graphics.Bitmap,X.07dw,X.0XRr,X.0YXb,X.0hvn,java.lang.Long,java.lang.String,java.lang.String,X.0Mho,X.02qk,X.1IAN,float,long,boolean)java.lang.Object`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A07()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1G()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1H()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1I()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A19()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1D()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1E()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1F()void`
- `android.app.Activity#onActivityResult(int,int,android.content.Intent)void`

Log:
```
   [InstantUploadHook.hookSwap:118] (IE|InstantUpload) ✅ capture handler hooked
   [InstantUploadHook.hookCameraOpen:239] (IE|InstantUpload) ✅ camera-open hooked (8 VM methods)
   [InstantUploadHook.hookPickerResult:686] (IE|InstantUpload) ✅ picker-result hooked
```

## OK — GhostEphemeralKeepHook

`new GhostEphemeralKeepHook().install(dexKitBridge, lpparam.classLoader)` (406 ms)

Hooked (6):
- `X.05s8#A0c(boolean)boolean`
- `X.05s8#A0a()boolean`
- `X.06da#H48(X.02su,X.0Mir,X.0AEc)void`
- `X.0005#A00(int)java.lang.String`
- `X.05oL#unsafeParseFromJson(X.06e4)java.lang.Object`
- `X.05oL#parseFromJsonParser(X.06e4)java.lang.Object`

Log:
```
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.05s8.A0c
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.05s8.A0a
   [GhostEphemeralKeepHook.hookServerPing:187] (IE|Ephemeral) ✅ server-ping hook → X.06da.H48
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.0005.A00
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.05oL.unsafeParseFromJson
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.05oL.parseFromJsonParser
```

## OK — GhostPermanentViewHook

`new GhostPermanentViewHook().install(dexKitBridge, lpparam.classLoader)` (169 ms)

Hooked (1):
- `X.03u1#unsafeParseFromJson(X.06e4)java.lang.Object`

Log:
```
   [GhostPermanentViewHook.install:98] (IE|ViewOnceMedia) ✅ hooking X.03u1.unsafeParseFromJson
   [GhostPermanentViewHook.install:105] (IE|ViewOnceMedia) ✅ hooked
```

## OK — ViewOnceBadgeHook

`new ViewOnceBadgeHook().install(dexKitBridge, lpparam.classLoader)` (80 ms)

Hooked (1):
- `X.0ADJ#<init>(X.07NC,X.014O,X.0AD2,com.instagram.direct.thread.analytics.DirectThreadAnalyticsParams,com.instagram.direct.wellbeing.harmfulcontent.ondevicenudity.view.data.PrivacyMediaOverlayViewModel,X.0HbO,java.lang.String,java.lang.String,boolean,boolean,boolean,boolean)`

Log:
```
   [ViewOnceBadgeHook.install:75] (IE|VOBadge) ✅ hooked X.0ADJ.<init>
```

## OK — LockedCollectionsHook

`new io.github.somilkhan.instaeclipse2.mods.ui.LockedCollectionsHook().install(hostClassLoader)` (51 ms)

Hooked (1):
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [LockedCollectionsHook.install:77] (IE|LockedColl) ✅ installed
```

## OK — SavedOnProfileHook

`new io.github.somilkhan.instaeclipse2.mods.ui.SavedOnProfileHook().install(dexKitBridge, hostClassLoader)` (67 ms)

Hooked (1):
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [SavedOnProfileHook.install:128] (IE|SavedProfile) ✅ X.0NV2.A00 / public static X.09DJ X.020R.A0Z(androidx.fragment.app.FragmentActivity,X.01yk)
```

## OK — HideHeaderButtonsHook

`new io.github.somilkhan.instaeclipse2.mods.ui.HideHeaderButtonsHook().install(hostClassLoader)` (52 ms)

Hooked (1):
- `android.app.Activity#onResume()void`

Log:
```
   [HideHeaderButtonsHook.install:46] (IE|HideButtons) ✅ installed
```

## OK — ShareLinkDownloadHook

`new io.github.somilkhan.instaeclipse2.mods.media.ShareLinkDownloadHook().install(hostClassLoader)` (52 ms)

Hooked (3):
- `com.instagram.common.session.UserSession#<init>(X.06t7,java.lang.String,X.02cj,boolean,boolean)`
- `android.app.Activity#onCreate(android.os.Bundle)void`
- `android.app.Activity#onNewIntent(android.content.Intent)void`

Log:
```
   [ShareLinkDownloadHook.install:75] (IE|ShareDL) ✅ installed
```

## OK — StoryDownloadHook

`new StoryDownloadHook().install(dexKitBridge, lpparam.classLoader)` (1389 ms)

Hooked (16):
- `X.0iNl#A0r(X.07qF,X.0iNl,X.0MZg,boolean)java.lang.CharSequence[]`
- `X.0iNl#A0s(X.0iNl)java.lang.CharSequence[]`
- `X.0iNl#A0t(X.0iNl,boolean)java.lang.CharSequence[]`
- `X.0iNl#A06(X.0iNl)java.util.ArrayList`
- `X.0iNl#A07(X.0iNl,boolean)java.util.ArrayList`
- `X.0hFY#A01(android.content.Context,X.0hFY)void`
- `X.0hFY#A03(X.0hFY,java.lang.CharSequence)void`
- `X.0iNl#A0F(android.content.Context,X.0aRA,com.instagram.model.reels.ReelItem,X.0iNl,X.0FSM)void`
- `X.0iNl#A0G(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0mjn,X.0mjo,X.0Uit,X.0mjs,X.0mjw,X.0iNl,X.0FSk,X.0FRP,X.0FVn,X.0FSO,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0I(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Tal,X.0Uit,X.0mjw,X.0iNl,X.0FTP,X.0FRP,X.0FSM,java.lang.CharSequence)void`
- `X.0iNl#A0J(android.content.DialogInterface$OnDismissListener,X.08Bg,X.0Uit,X.0mqm,X.0mju,X.0iNl,X.0FVn,X.0FUP,X.0FSM,X.0FVP,java.lang.CharSequence)void`
- `X.0iNl#A0d(java.util.List)void`
- `X.0iNl#A0u(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Bg,X.0hlw,X.0Uit,X.0mqm,X.0mju,X.0mjv,X.0FVn,X.0FUP,X.0FSM,X.0FVP,X.0FVO)void`
- `X.0hGu#A0H(android.view.View,X.0mpy,com.instagram.model.reels.ReelItem,java.lang.Integer)void`
- `X.0R1O#A00()java.util.List`
- `instagram.features.stories.fragment.ReelViewerFragment#FI9(com.instagram.model.reels.ReelItem,X.0VAE)void`

Log:
```
   [StoryDownloadHook.install:199] (IE|Highlight) reel class X.04nk
   [StoryDownloadHook.installButtonInjectorHook:606] (IE|Story) button injector hooked 3 builder(s)
   [StoryDownloadHook.installHighlightButtonInjectorHook:671] (IE|Story) Highlight injector hooked 2 builder(s)
   [StoryDownloadHook.installClickHandlerHook:805] (IE|Story) click handler hooked 8 dispatcher(s)
   [StoryDownloadHook.installHighlightTrayHook:297] (IE|HighlightTray) ✅ X.0hGu legacy=true popup=false loader=false
   [StoryDownloadHook.installStoryCaptureHook:455] (IE|StoryCache) capture hook: 1 method(s)
```

## OK — ReelDownloadHook

`new ReelDownloadHook().install(dexKitBridge, lpparam.classLoader)` (482 ms)

Hooked (5):
- `X.091g#A06(X.0Zoz,X.03yF,X.091g)java.util.ArrayList`
- `X.0CfI#A07(X.0CgA,com.instagram.common.session.UserSession)boolean`
- `X.091g#A0B(android.graphics.PointF,android.view.View,android.view.View,X.03yF,X.09IE,X.0KID,X.091g)void`
- `X.091g#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0R1O)void`
- `X.091g#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0R1O)void`

Log:
```
   [ReelDownloadHook.installGateHook:507] (IE|Reel) gate config 36313978552585585 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installGateHook:507] (IE|Reel) gate config 36313978552847731 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installReduceOptionsListPatch:245] (IE|Reel) ✅ Options-list patch hooked: X.091g.A06
   [ReelDownloadHook.install:81] (IE|Reel) string matches=4 candidateClasses=3
   [ReelDownloadHook.installEligibilityGate457:424] (IE|Reel) ✅ 457 eligibility gate: X.0CfI.A07
   [ReelDownloadHook.installSheetRowInjection:368] (IE|Reel) ✅ sheet row injection: X.091g builders=1 rowAdders=2
```

## OK — ProfilePicDownloadHook

`ProfilePicDownloadHook.install(dexKitBridge, hostClassLoader)` (68 ms)

Hooked (2):
- `androidx.fragment.app.Fragment#onResume()void`
- `android.view.View#onAttachedToWindow()void`

Log:
```
   [ProfilePicDownloadHook.resolveHdGetter:118] (IE|ProfileDL) ✅ HD url via X.02fy hash(hd_profile_pic_url_info)
```

## OK — interceptor

`interceptor.handleInterceptor(lpparam)` (65 ms)

Hooked (2):
- `com.instagram.api.tigon.TigonServiceLayer#startRequest(X.03rA,X.03rm,X.03ta)X.0Eul`
- `X.01yy#startRequest(X.03rA,X.03rm,X.03ta)X.0Eul`

Log:
```
   [IGNetworkInterceptor.hookChainEntry:76] (InstaEclipse | Interceptor): chain-entry hooks: 1
```

## OK — TerminatedExecutorGuard

`new io.github.somilkhan.instaeclipse2.mods.core.TerminatedExecutorGuard().install(lpparam.classLoader)` (51 ms)

Hooked (1):
- `java.util.concurrent.ThreadPoolExecutor$AbortPolicy#rejectedExecution(java.lang.Runnable,java.util.concurrent.ThreadPoolExecutor)void`

Log:
```
   [TerminatedExecutorGuard.install:53] (IE|ExecGuard) ✅ installed
```

