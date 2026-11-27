# hook-verify report

APK: `ig458_1.apk` — Instagram 458.0.0.0.67 (386405730)

Results hold for this exact build only; a device running another build of the same major version can resolve different classes.

Status legend: **OK** = hooks installed and no failure lines; **PARTIAL** = some hooks installed but at least one sub-lookup logged a failure; **FAIL** = failure logged and nothing hooked; **NO-HOOK** = nothing hooked and nothing logged (usually a hook that installs lazily at runtime, or one that only hooks framework classes once an Activity exists); **ERROR** = install threw. **(harness)** = every failure line comes from the offline harness (stubbed Instagram code or mockable android.jar), not from a lookup; confirm on a device.

```
OK       instagramUI                      hooks=5      504 ms
OK       DevOptionsUnlockHook             hooks=1     1145 ms
OK       GhostDMSeenHook                  hooks=1      188 ms
OK       GhostDMMarkAsReadHook            hooks=1       52 ms
OK       GhostChannelMarkAsReadHook       hooks=1       52 ms
OK       GhostTypingIndicatorHook         hooks=1      189 ms
OK       GhostScreenshotDetectionHook     hooks=1      164 ms
OK       ScreenshotPermissionHook         hooks=2       55 ms
OK       GhostViewOnceHook                hooks=1      190 ms
OK       GhostStorySeenHook               hooks=6      179 ms
OK       KeepUnsentMessagesHook           hooks=7      833 ms
OK       UnsentThreadButtonHook           hooks=5       58 ms
OK       HideChatsHook                    hooks=3      317 ms
OK       CustomFontHook                   hooks=9       83 ms
OK       metaAi                           hooks=2       53 ms
OK       metaAi                           hooks=8     2421 ms
OK       DisableRepostHook                hooks=2      677 ms
OK       LockDirectMessagesHook           hooks=1       53 ms
NO-HOOK  HideSuggestedFeedItemsHook       hooks=0     1149 ms
OK       AdBlocker                        hooks=1      188 ms
OK       TrackingLinkDisable              hooks=5       55 ms
OK       ExactTimestampHook               hooks=1      156 ms
OK       ExpandCaptionsHook               hooks=2      253 ms
OK       HideCommunityNotesHook           hooks=2       64 ms
OK       ReelsControlsHook                hooks=3     1011 ms
OK       AccountLimitHook                 hooks=1     1473 ms
OK       AirplaneModeHook                 hooks=3       54 ms
OK       HighResolutionImageHook          hooks=1      171 ms
OK       OnboardingPromptHook             hooks=1      183 ms
NO-HOOK  NotificationRegistrationHook     hooks=0       51 ms
OK       StartupTabHook                   hooks=4       53 ms
OK       SwipeToCameraHook                hooks=1       53 ms
OK       FollowingOnlyFeedHook            hooks=5      311 ms
OK       ProfileFollowsYouHook            hooks=2      173 ms
PARTIAL  PlaybackSpeedHook                hooks=24    2154 ms
OK       HideNotesTrayHook                hooks=1      221 ms
PARTIAL  VoiceMessageDownloadHook         hooks=6      694 ms  (harness)
OK       EditHistoryHook                  hooks=2      248 ms
NO-HOOK  JumpToFirstMessageHook           hooks=0       51 ms
NO-HOOK  ReadReceiptChatHook              hooks=0       52 ms
OK       StoryViewerSearchHook            hooks=1      341 ms
OK       CommentSearchHook                hooks=2      516 ms
OK       ChatExportHook                   hooks=1      376 ms
OK       FollowListExportHook             hooks=1      372 ms
OK       LikeUndoHook                     hooks=1     2190 ms
OK       ShareSheetCleanupHook            hooks=2       57 ms
NO-HOOK  ExternalBrowserHook              hooks=0       51 ms
OK       DisableStoryFlippingHook         hooks=1      134 ms
OK       StoryMentionHook                 hooks=11     617 ms
OK       StoryLinksHook                   hooks=11     390 ms
OK       StoryInfoHook                    hooks=11    1525 ms
OK       CommentCopyHook                  hooks=4      314 ms
OK       CaptionCopyContextMenuHook       hooks=11    1416 ms
OK       DisableDoubleTapLikeHook         hooks=2      875 ms
OK       DoubleTapExtrasHook              hooks=6      337 ms
OK       FeedPhotoZoomHook                hooks=1       55 ms
FAIL     LocationSpoofHook                hooks=0       52 ms  (harness)
OK       IgThemeHook                      hooks=21      65 ms
OK       ForceReelQualityHook             hooks=11     106 ms
OK       MaxUploadQualityHook             hooks=1      492 ms
OK       DisableVideoAutoPlayHook         hooks=9      187 ms
OK       BuildExpiredPopupHook            hooks=2      301 ms
NO-HOOK  FeedVideoDownloadHook            hooks=0       58 ms
OK       FeedVideoDownloadHook            hooks=2     2256 ms
OK       PostDownloadContextMenuHook      hooks=5      414 ms
OK       InstantSaveHook                  hooks=2       53 ms
OK       InstantUploadHook                hooks=10      56 ms
OK       GhostEphemeralKeepHook           hooks=7      451 ms
OK       GhostPermanentViewHook           hooks=1      186 ms
OK       ViewOnceBadgeHook                hooks=1       81 ms
OK       LockedCollectionsHook            hooks=1       51 ms
OK       SavedOnProfileHook               hooks=1       59 ms
OK       HideHeaderButtonsHook            hooks=1       51 ms
OK       ShareLinkDownloadHook            hooks=3       53 ms
OK       StoryDownloadHook                hooks=16    1047 ms
OK       ReelDownloadHook                 hooks=5      480 ms
OK       ProfilePicDownloadHook           hooks=2       67 ms
OK       interceptor                      hooks=2       68 ms
OK       TerminatedExecutorGuard          hooks=1       51 ms
TOTAL: 79 steps  OK=70 PARTIAL=2 FAIL=1 NO-HOOK=6 ERROR=0
```

## OK — instagramUI

`instagramUI.mainActivity(hostClassLoader)` (504 ms)

Hooked (5):
- `com.instagram.base.activity.BaseFragmentActivity#onCreate(android.os.Bundle)void`
- `com.instagram.base.activity.IgFragmentActivity#onResume()void`
- `com.instagram.mainactivity.InstagramMainActivity#BD9()X.02vS`
- `android.view.View#performLongClick()boolean`
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [UIHookManager.mainActivity:184] (InstaEclipse): ✅ main onCreate hooked via com.instagram.base.activity.BaseFragmentActivity#onCreate
   [UIHookManager.mainActivity:201] (InstaEclipse): ✅ main onResume hooked via com.instagram.base.activity.IgFragmentActivity#onResume
   [BottomSheetHookUtil.hookMethod:75] (InstaEclipse | BottomSheet): ✅ Hooked: com.instagram.mainactivity.InstagramMainActivity.BD9
```

## OK — DevOptionsUnlockHook

`new DevOptionsUnlockHook().handleDevOptions(dexKitBridge)` (1145 ms)

Hooked (1):
- `X.02uw#A01(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DevOptionsUnlockHook.findAndHookDynamicMethod:73] (InstaEclipse | DevOptionsEnable): 🔍 Discovery Tier 1 (String)...
   [DevOptionsUnlockHook.findAndHookDynamicMethod:105] (InstaEclipse | DevOptionsEnable): ℹ️ Tier 1 (String) found no gate on this build, trying Tier 2 (Structural)...
   [DevOptionsUnlockHook.hookExactMethod:224] (InstaEclipse | DevOptionsEnable): ✅ Hooked: X.02uw.A01
   [DevOptionsUnlockHook.findAndHookDynamicMethod:112] (InstaEclipse | DevOptionsEnable): 🎯 Found via structural match: X.02uw.A01
```

## OK — GhostDMSeenHook

`new GhostDMSeenHook().handleSeenBlock(dexKitBridge)` (188 ms)

Hooked (1):
- `X.0H8g#A0A(com.instagram.common.session.UserSession,X.05qL,java.lang.String,java.lang.String,java.lang.String)void`

Log:
```
   [GhostDMSeenHook.handleSeenBlock:86] (InstaEclipse | GhostModeSeen): ✅ Hooked: X.0H8g.A0A
```

## OK — GhostDMMarkAsReadHook

`new GhostDMMarkAsReadHook(moduleSourceDir).install(lpparam.classLoader)` (52 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostChannelMarkAsReadHook

`new GhostChannelMarkAsReadHook().install(lpparam.classLoader)` (52 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostTypingIndicatorHook

`new GhostTypingIndicatorHook().handleTypingBlock(dexKitBridge)` (189 ms)

Hooked (1):
- `X.04YC#A0S(boolean)void`

Log:
```
   [GhostTypingIndicatorHook.handleTypingBlock:86] (InstaEclipse | TypingBlock): ✅ Hooked (dynamic check): X.04YC.A0S
```

## OK — GhostScreenshotDetectionHook

`new GhostScreenshotDetectionHook().handleScreenshotBlock(dexKitBridge)` (164 ms)

Hooked (1):
- `X.05TN#GEz(long)void`

Log:
```
   [GhostScreenshotDetectionHook.handleScreenshotBlock:74] (InstaEclipse | ScreenshotBlock): ✅ Hooked (dynamic check): X.05TN.GEz
```

## OK — ScreenshotPermissionHook

`new ScreenshotPermissionHook().install(lpparam.classLoader)` (55 ms)

Hooked (2):
- `android.view.Window#setFlags(int,int)void`
- `android.view.Window#addFlags(int)void`

Log:
```
   [ScreenshotPermissionHook.install:47] (InstaEclipse | ScreenshotPermission): ✅ Hooked Window.setFlags + addFlags
```

## OK — GhostViewOnceHook

`new GhostViewOnceHook().handleViewOnceBlock(dexKitBridge)` (190 ms)

Hooked (1):
- `X.05rh#H84(X.02tm,X.0faP,X.0FeC)void`

Log:
```
   [GhostViewOnceHook.tryHookViewOnce:77] (InstaEclipse | ViewOnce): ✅ Hooked (endpoint+raven_media): X.05rh.H84
```

## OK — GhostStorySeenHook

`new GhostStorySeenHook().handleStorySeenBlock(dexKitBridge)` (179 ms)

Hooked (6):
- `X.00E3#A0E(java.lang.String,java.lang.Object)void`
- `X.00E3#A0F(java.util.Map)void`
- `X.00E3#A0R(java.lang.String,java.lang.Object)void`
- `X.02nQ#A0K(java.lang.Object)void`
- `X.02nQ#A0S(X.02nR)void`
- `X.02nQ#A0T(X.02nR)void`

Log:
```
   [GhostStorySeenHook.tryModernBatchBlock:103] (InstaEclipse | StoryBlock): ✅ Hooked 6 sender(s) on store X.02nQ (item builder: X.02nR.A06)
```

## OK — KeepUnsentMessagesHook

`new KeepUnsentMessagesHook().install(dexKitBridge, lpparam.classLoader)` (833 ms)

Hooked (7):
- `androidx.fragment.app.Fragment#performResume()void`
- `androidx.fragment.app.Fragment#performPause()void`
- `androidx.fragment.app.Fragment#performDestroyView()void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#Gws(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String)void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#Gwt(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String,boolean)void`
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
   [KeepUnsentMessagesHook.resolveEntryClass:395] (IE|KeepUnsent) ✅ thread entry → X.05s9
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.Gws (3-arg)
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.Gwt (4-arg)
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

`new io.github.somilkhan.instaeclipse2.mods.ghost.HideChatsHook().install(dexKitBridge, lpparam.classLoader)` (317 ms)

Hooked (3):
- `X.004Z#<init>(android.content.Context,X.04nw,com.instagram.common.session.UserSession,com.instagram.direct.store.impl.DirectThreadStoreImpl,com.instagram.direct.store.impl.sqlite.DirectSQLiteDiskIO,X.003T,java.lang.Object)`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0d(X.00B1,X.0Mue,java.util.Comparator,java.util.List,boolean)java.util.ArrayList`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0e(java.util.List)java.util.ArrayList`

Log:
```
   [HiddenInboxFilter.installRefresh:121] (IE|HideChats) local inbox refresh method resolved
   [HiddenInboxFilter.install:93] (IE|HideChats) inbox methods resolved=2; execution requires a real inbox
```

## OK — CustomFontHook

`new io.github.somilkhan.instaeclipse2.mods.ui.CustomFontHook().install(lpparam.classLoader)` (83 ms)

Hooked (9):
- `android.graphics.Typeface#createFromAsset(android.content.res.AssetManager,java.lang.String)android.graphics.Typeface`
- `android.graphics.Typeface#createFromFile(java.io.File)android.graphics.Typeface`
- `android.graphics.Typeface#createFromFile(java.lang.String)android.graphics.Typeface`
- `android.widget.TextView#setTypeface(android.graphics.Typeface)void`
- `android.widget.TextView#setTypeface(android.graphics.Typeface,int)void`
- `android.graphics.Typeface$Builder#build()android.graphics.Typeface`
- `android.graphics.Typeface$CustomFallbackBuilder#build()android.graphics.Typeface`
- `android.app.Activity#onActivityResult(int,int,android.content.Intent)void`
- `X.00ab#run()void`

Log:
```
   [CustomFontHook.installFontHooks:55] (IE|Font) ✅ installed
   [CustomFontHook.installEmojiWorkerHook:395] (IE|Emoji) ✅ hook installed (emoji2 loader worker x1)
```

## OK — metaAi

`metaAi.install(lpparam.classLoader)` (53 ms)

Hooked (2):
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup)android.view.View`

Log:
```
   [RemoveMetaAIHook.install:126] (IE|RemoveMetaAI) ✅ installed
```

## OK — metaAi

`metaAi.installReels(dexKitBridge, lpparam.classLoader)` (2421 ms)

Hooked (8):
- `X.0PBv#A01(boolean)java.util.List`
- `X.08Nn#A06(X.0LCC,X.03y5,X.08Nn)java.util.ArrayList`
- `X.08Nw#A0c(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0fhT,X.0KHl)void`
- `X.08Nn#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0RDk)void`
- `X.08Nw#A09(X.0LCC,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0fhT,X.08Nw)void`
- `X.08Nn#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0RDk)void`
- `X.08Nw#A0d(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0fhT,X.0KHl,java.lang.Integer,java.lang.String)void`
- `X.08Nw#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`

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

`new io.github.somilkhan.instaeclipse2.mods.ui.DisableRepostHook().install(dexKitBridge, lpparam.classLoader)` (677 ms)

Hooked (2):
- `X.0MzW#E70(android.view.View,X.08gD,X.0B3k,java.lang.String,java.lang.String,java.lang.String,kotlin.jvm.functions.Function0,int)void`
- `X.071d#A0Q(X.0KhY,kotlin.jvm.functions.Function1)void`

Log:
```
   [DisableRepostHook.installFeedCommit:156] (IE|Repost) feed postRepost launcher: 1 method(s)
   [DisableRepostHook.install:73] (IE|Repost) ✅ installed: 2 method(s)
```

## OK — LockDirectMessagesHook

`new io.github.somilkhan.instaeclipse2.mods.ui.LockDirectMessagesHook().install(lpparam.classLoader)` (53 ms)

Hooked (1):
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [LockDirectMessagesHook.install:61] (IE|LockDMs) ✅ installed
```

## OK — HideSuggestedFeedItemsHook (after the parser fix; re-run with --filter HideSuggested: hooked X.06ju unsafeParseFromJson + parseFromJsonParser)

`new HideSuggestedFeedItemsHook().install(dexKitBridge, hostClassLoader)` (1149 ms)

Log:
```
   [HideSuggestedFeedItemsHook.resolveHasLikedGetter:157] (InstaEclipse | HideLiked): ✅ has_liked via BvS(hash)
   [HideSuggestedFeedItemsHook.install:122] (InstaEclipse | HideSuggested): ✅ Hooked: X.055h
```

## OK — AdBlocker

`new AdBlocker().disableSponsoredContent(dexKitBridge, hostClassLoader)` (188 ms)

Hooked (1):
- `X.06mP#A02(X.06mP,X.03x5,X.05AC)boolean`

Log:
```
   [AdBlocker.disableSponsoredContent:68] (InstaEclipse | AdBlocker): ✅ Hooked (dynamic check, marker='Is ad pod'): X.06mP.A02
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

`new io.github.somilkhan.instaeclipse2.mods.misc.ExactTimestampHook().install(dexKitBridge)` (156 ms)

Hooked (1):
- `X.03hd#A08(android.content.res.Resources,X.05uC,X.03hd,java.lang.Integer,double,double,boolean,boolean,boolean,boolean)java.lang.String`

Log:
```
   [ExactTimestampHook.install:84] (InstaEclipse | ExactTime): ✅ Hooked X.03hd.A08
```

## OK — ExpandCaptionsHook

`new io.github.somilkhan.instaeclipse2.mods.feed.ExpandCaptionsHook().install(dexKitBridge)` (253 ms)

Hooked (2):
- `X.02mR#A03(android.content.Context,X.00r2,com.instagram.search.common.analytics.SearchContext,X.02mR,java.lang.String,java.lang.String,boolean)android.text.SpannableStringBuilder`
- `X.01BZ#A00(X.02mb,java.lang.CharSequence,java.lang.CharSequence,java.lang.CharSequence,int,boolean)java.lang.CharSequence`

Log:
```
   [ExpandCaptionsHook.install:67] (InstaEclipse | ExpandCaptions): ✅ Hooked X.02mR.A03 → X.01BZ.A00
```

## OK — HideCommunityNotesHook

`new io.github.somilkhan.instaeclipse2.mods.feed.HideCommunityNotesHook().install(dexKitBridge, hostClassLoader)` (64 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoMediaOverlayPayloadSchema#Cgj()X.0eFz`
- `com.instagram.api.schemas.MediaOverlayPayloadSchema#Cgj()X.0eFz`

Log:
```
   [HideCommunityNotesHook.install:71] (InstaEclipse | CommunityNotes): ✅ notes getter Cgj hooked on 2 class(es)
```

## OK — ReelsControlsHook

`new io.github.somilkhan.instaeclipse2.mods.extras.ReelsControlsHook().install(dexKitBridge, hostClassLoader)` (1011 ms)

Hooked (3):
- `X.06EG#A09(com.instagram.common.session.UserSession,boolean)boolean`
- `X.087h#Dx2(int)android.view.View`
- `instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout#onInterceptTouchEvent(android.view.MotionEvent)boolean`

Log:
```
   [ReelsControlsHook.installAutoScroll:66] (InstaEclipse | Reels): ✅ auto-scroll gate X.06EG.A09
   [ReelsControlsHook.installScrollLock:97] (InstaEclipse | Reels): ✅ pager X.087h.A0H
```

## OK — AccountLimitHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AccountLimitHook().install(dexKitBridge, hostClassLoader)` (1473 ms)

Hooked (1):
- `X.08rP#A01(X.06t4,int)boolean`

Log:
```
   [AccountLimitHook.install:38] (InstaEclipse | UnlimitedAccounts): ✅ hooked X.08rP.A01
```

## OK — AirplaneModeHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AirplaneModeHook().install(hostClassLoader)` (54 ms)

Hooked (3):
- `com.instagram.realtimeclient.RealtimeClientManager#<init>(android.content.Context,com.instagram.common.session.UserSession,com.instagram.realtimeclient.clientconfig.RealtimeClientConfig,com.instagram.realtimeclient.MainRealtimeEventHandler,X.0Mem)`
- `com.instagram.realtimeclient.RealtimeClientManager#startMqttClient()void`
- `com.instagram.realtimeclient.MainRealtimeEventHandler#onRealtimeEvent(java.lang.String,com.instagram.realtimeclient.RealtimeEvent)void`

Log:
```
   [AirplaneModeHook.install:84] (InstaEclipse | Airplane): ✅ hooked
```

## OK — HighResolutionImageHook

`new io.github.somilkhan.instaeclipse2.mods.extras.HighResolutionImageHook().install(dexKitBridge, hostClassLoader)` (171 ms)

Hooked (1):
- `X.05yM#A02(java.lang.Integer,java.util.List,int)com.instagram.common.typedurl.ImageUrl`

Log:
```
   [HighResolutionImageHook.install:88] (InstaEclipse | HighResImages): ✅ hooked 1 picker(s)
```

## OK — OnboardingPromptHook

`new io.github.somilkhan.instaeclipse2.mods.extras.OnboardingPromptHook().install(dexKitBridge, hostClassLoader)` (183 ms)

Hooked (1):
- `X.0gda#A06(android.content.Context,com.instagram.bloks.hosting.IgBloksScreenConfig)void`

Log:
```
   [OnboardingPromptHook.install:77] (InstaEclipse | Onboarding): ✅ hooked 1 launcher(s)
```

## NO-HOOK — NotificationRegistrationHook

`new io.github.somilkhan.instaeclipse2.mods.extras.NotificationRegistrationHook().install()` (51 ms)

## OK — StartupTabHook

`new io.github.somilkhan.instaeclipse2.mods.ui.StartupTabHook().install(hostClassLoader)` (53 ms)

Hooked (4):
- `android.app.Instrumentation#callActivityOnCreate(android.app.Activity,android.os.Bundle)void`
- `android.app.Instrumentation#callActivityOnCreate(android.app.Activity,android.os.Bundle,android.os.PersistableBundle)void`
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

`new io.github.somilkhan.instaeclipse2.mods.feed.FollowingOnlyFeedHook().install(dexKitBridge, hostClassLoader)` (311 ms)

Hooked (5):
- `X.03dp#A02(android.content.Context,X.06j1,X.0EA5,com.instagram.common.session.UserSession,X.03cy,X.02wb)void`
- `X.06j1#A0L(java.lang.String,java.lang.String)void`
- `X.06j1#ACU(java.lang.String,java.lang.String)void`
- `X.06j1#ACm(java.lang.String,java.lang.String)void`
- `X.06j1#ABA(java.lang.String,java.lang.String)void`

Log:
```
   [FollowingOnlyFeedHook.install:129] (InstaEclipse | FollowingFeed): ✅ hooked X.03dp.A02 + 4 adders
```

## OK — ProfileFollowsYouHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ProfileFollowsYouHook().install(dexKitBridge, hostClassLoader)` (173 ms)

Hooked (2):
- `com.instagram.profile.fragment.UserDetailFragment#A0n(com.instagram.profile.fragment.UserDetailFragment,boolean)void`
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [IgModels.ensure:104] (IE|Models) ✅ User model = X.02gr
   [ProfileFollowsYouHook.install:102] (InstaEclipse | FollowsYou): ✅ Hooked A0n / followed_by via hashed accessor
```

## PARTIAL — PlaybackSpeedHook

`new io.github.somilkhan.instaeclipse2.mods.media.PlaybackSpeedHook().install(dexKitBridge, hostClassLoader)` (2154 ms)

Hooked (24):
- `X.03Ze#A0R(com.instagram.feed.media.Media,X.0CaD,X.0Kef,X.08gD,X.0Keo,int,boolean)boolean`
- `X.03BL#Gde(long,long)void`
- `X.0AdI#HTm(float)void`
- `X.0AdI#<init>(X.06n9,com.instagram.common.session.UserSession,X.00o3,X.0Add,java.lang.String)`
- `X.0CJP#HTm(float)void`
- `X.0CJP#<init>(android.content.Context,com.instagram.common.session.UserSession,X.07pW,X.00o3,X.0Saj)`
- `X.0CLp#HTm(float)void`
- `X.0CLp#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0CaD,X.0Ra6,X.0CLn,kotlin.jvm.functions.Function1,boolean,boolean)`
- `X.0CdK#HTm(float)void`
- `X.0CdK#<init>(android.content.Context,com.instagram.common.session.UserSession,X.07pW,X.00o3,X.0Saj)`
- `X.0D55#HTm(float)void`
- `X.0SY5#HTm(float)void`
- `X.0SY5#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0CaD,java.lang.String)`
- `X.03Ei#A0D(X.03Ei,float,float)void`
- `X.03Ei#A02(X.03Ei)void`
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
   [PlaybackSpeedHook.installFeed:512] (InstaEclipse | PlaybackSpeed): ✅ feed player X.03Ze.A0R, started-cb X.03BL.Gde, setters 1
   [PlaybackSpeedHook.installHero:268] (InstaEclipse | PlaybackSpeed): ✅ hero X.03Ei.A0D, prepare A02
!! [PlaybackSpeedHook.installReelScrubber:585] (InstaEclipse | PlaybackSpeed): ⚠️ reel scrubber gate not found
   [PlaybackSpeedHook.installHoldSide:153] (InstaEclipse | PlaybackSpeed): ✅ hold side tracking
```

## OK — HideNotesTrayHook

`new io.github.somilkhan.instaeclipse2.mods.ui.HideNotesTrayHook().install(dexKitBridge)` (221 ms)

Hooked (1):
- `X.0CHD#invoke(java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [HideNotesTrayHook.installTaggedTray:112] (InstaEclipse | NotesTray): ✅ tagged tray X.0CHD.invoke
```

## PARTIAL — VoiceMessageDownloadHook

`new io.github.somilkhan.instaeclipse2.mods.media.VoiceMessageDownloadHook().install(dexKitBridge, hostClassLoader)` (694 ms)

Hooked (6):
- `X.0A9i#A00(X.0A9i,java.lang.String,java.lang.String,boolean)X.05oE`
- `com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel#A00(android.content.Context,android.graphics.PointF,android.graphics.Rect,com.instagram.aistudio.logging.AiStudioLoggingData,com.instagram.common.session.UserSession,X.03XB,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,java.util.List,java.util.List,int,int,long,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean)com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel`
- `X.0KdK#A08(android.app.Activity,android.view.View,com.instagram.common.session.UserSession,X.0Muv,X.0Mfc,X.0Mfe,X.0Mpr,X.0Mfg,X.0Mre,X.0Mmu,X.0fAI,X.0Mfr,X.0Mfv,X.0Mfx,X.0Mgi,X.0Mpt,X.0Mha,X.0Mhg,X.0Mhh,X.0Mni,X.0Mhi,X.0Mhm,X.0Mhn,X.0Mho,X.0Mas,X.0Mpw,X.0Mht,X.0Mrg,X.0Mpy,X.0MiA,X.0Mnn,X.0eka,X.0MqA,X.0CgN,X.0GbO,X.05oE,com.instagram.model.direct.DirectThreadKey,X.03XB,com.instagram.model.direct.messageid.MessageIdentifier,X.0Hb1,X.05En,X.05En,X.05En,X.05En,X.05En,X.0HbP,X.0Ksx,X.0Ksx,X.0Ksx,X.0Ksx,X.0Ksx,X.0Ksx,X.0HoL,X.05EY,X.05EY,X.0HrN,java.lang.String,java.lang.String,java.lang.String,java.util.List,kotlin.jvm.functions.Function0,int,long)void`
- `X.0aTy#A02(com.instagram.direct.messagethread.interaction.longpressaction.LongPressActionData,X.0aTy,java.lang.String)void`
- `X.0Ktj#E2e(X.0GbO,java.lang.String)void`
- `X.0any#E2e(X.0GbO,java.lang.String)void`

Log:
```
!! [VoiceMessageDownloadHook.install:383] (IE|VoiceDL) ⚠️ compose rows: java.lang.reflect.InvocationTargetException / java.lang.UnsupportedOperationException: hook-verify stub: Instagram code does not run on the host JVM
   [VoiceMessageDownloadHook.install:385] (IE|VoiceDL) compose row hooks=0 (label field ok)
   [VoiceMessageDownloadHook.install:386] (IE|VoiceDL) ✅ lookups=1 factories=1 dispatchers=4 mapper=true message=X.05oE
   [BulkUnsend.install:142] (IE|BulkUnsend) ✅ unsend=X.05Ff.A0I ref=X.0XIN
```

## OK — EditHistoryHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.EditHistoryHook().install(dexKitBridge, hostClassLoader)` (248 ms)

Hooked (2):
- `X.05oE#A18(com.instagram.common.session.UserSession,X.05oE,X.03Xr)void`
- `X.05oE#A10()java.util.List`

Log:
```
   [EditHistoryHook.install:149] (IE|EditHistory) ✅ merge A18, history getter A10
```

## NO-HOOK — JumpToFirstMessageHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.JumpToFirstMessageHook().install()` (51 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.ui.StoryViewerSearchHook().install(dexKitBridge, hostClassLoader)` (341 ms)

Hooked (1):
- `X.0RxC#A07(java.util.List,boolean,boolean,boolean)void`

Log:
```
   [StoryViewerSearchHook.install:105] (IE|ViewerSearch) ✅ adapter X.0RxC.A07 rebuild=A13
```

## OK — CommentSearchHook

`new io.github.somilkhan.instaeclipse2.mods.misc.CommentSearchHook().install(dexKitBridge, hostClassLoader)` (516 ms)

Hooked (2):
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#A07(X.01H4,X.01H3,X.01H3,X.01I3,X.01T0,X.01Z7,X.0200,X.0161,X.01F8,X.01W8,com.instagram.comments.mvvm.viewmodel.CommentListViewModel,X.0OlR,X.0Mc5,X.0Nrl,boolean,boolean,boolean)X.0OzS`
- `X.0UXU#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [CommentSearchHook.install:131] (IE|CommentSearch) comment json: serializer=true factory=true
   [CommentSearchHook.install:152] (IE|CommentSearch) ✅ vm com.instagram.comments.mvvm.viewmodel.CommentListViewModel builders=1 fragments=1
```

## OK — ChatExportHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.ChatExportHook().install(dexKitBridge, hostClassLoader)` (376 ms)

Hooked (1):
- `X.05s9#<init>(com.instagram.common.session.UserSession,X.05pg,java.util.List)`

Log:
```
   [ChatExportHook.install:125] (IE|ChatExport) ✅ store X.05s9 messages=true serializer=true factory=X.02dg
```

## OK — FollowListExportHook

`new io.github.somilkhan.instaeclipse2.mods.misc.FollowListExportHook().install(dexKitBridge, hostClassLoader)` (372 ms)

Hooked (1):
- `X.0CIA#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [FollowListExportHook.findPager:233] (IE|FollowExport) pager=X.0CLX loadMore=AL6
   [FollowListExportHook.install:107] (IE|FollowExport) ✅ X.0CIA fullName=false
```

## OK — LikeUndoHook

`new io.github.somilkhan.instaeclipse2.mods.misc.LikeUndoHook().install(dexKitBridge)` (2190 ms)

Hooked (1):
- `X.0PSG#A03(android.content.Context,X.08Fy,com.instagram.common.session.UserSession,X.0UQm,com.instagram.feed.media.Media,X.00f2,com.instagram.search.common.analytics.SearchContext,java.lang.Double,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.Map,boolean)void`

Log:
```
   [LikeUndoHook.install:130] (InstaEclipse | LikeUndo): ✅ Hooked X.0PSG.A03
```

## OK — ShareSheetCleanupHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ShareSheetCleanupHook().install(hostClassLoader)` (57 ms)

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

`new DisableStoryFlippingHook().handleStoryFlippingDisable(dexKitBridge)` (134 ms)

Hooked (1):
- `instagram.features.stories.fragment.ReelViewerFragment#A1V(com.instagram.model.reels.ReelItem)void`

Log:
```
   [DisableStoryFlippingHook.findAndHookMethod:104] (InstaEclipse | StoryFlipping): ✅ Hooked (dynamic check): instagram.features.stories.fragment.ReelViewerFragment.A1V
```

## OK — StoryMentionHook

`new StoryMentionHook().install(dexKitBridge, lpparam.classLoader)` (617 ms)

Hooked (11):
- `X.0pgh#A0q(X.07pW,X.0pgh,X.0HWw,boolean)java.lang.CharSequence[]`
- `X.0pgh#A0r(X.0pgh)java.lang.CharSequence[]`
- `X.0pgh#A0s(X.0pgh,boolean)java.lang.CharSequence[]`
- `X.0okZ#A01(android.content.Context,X.0okZ)void`
- `X.0okZ#A03(X.0okZ,java.lang.CharSequence)void`
- `X.0pgh#A0F(android.content.Context,X.0fhT,com.instagram.model.reels.ReelItem,X.0pgh,X.0EmY)void`
- `X.0pgh#A0G(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0tpp,X.0tql,X.0UNm,X.0trl,X.0tro,X.0pgh,X.0Emy,X.0EmW,X.0EnW,X.0Emr,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0I(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0UNm,X.0tro,X.0pgh,X.0EnR,X.0EmW,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0J(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0UNm,X.0uln,X.0trm,X.0pgh,X.0EnW,X.0EnT,X.0EmY,X.0EnV,java.lang.CharSequence)void`
- `X.0pgh#A0d(java.util.List)void`
- `X.0pgh#A0t(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Fy,X.0fat,X.0UNm,X.0uln,X.0trm,X.0Qni,X.0EnW,X.0EnT,X.0EmY,X.0EnV,X.0EnU)void`

Log:
```
   [StoryMentionHook.resolveMentionPipeline:119] (IE|Mention) ✅ reel_mentions via hashed accessor
   [StoryMentionHook.resolveMentionPipeline:165] (IE|Mention) ✅ pipeline resolved: hash(reel_mentions) -> A0E
   [StoryMentionHook.installButtonHook:245] (IE|Mention) ✅ button hook installed on 3 builder(s)
   [StoryMentionHook.installClickHook:300] (IE|Mention) ✅ click hook installed
```

## OK — StoryLinksHook

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryLinksHook().install(dexKitBridge, lpparam.classLoader)` (390 ms)

Hooked (11):
- `X.0pgh#A0q(X.07pW,X.0pgh,X.0HWw,boolean)java.lang.CharSequence[]`
- `X.0pgh#A0r(X.0pgh)java.lang.CharSequence[]`
- `X.0pgh#A0s(X.0pgh,boolean)java.lang.CharSequence[]`
- `X.0okZ#A01(android.content.Context,X.0okZ)void`
- `X.0okZ#A03(X.0okZ,java.lang.CharSequence)void`
- `X.0pgh#A0F(android.content.Context,X.0fhT,com.instagram.model.reels.ReelItem,X.0pgh,X.0EmY)void`
- `X.0pgh#A0G(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0tpp,X.0tql,X.0UNm,X.0trl,X.0tro,X.0pgh,X.0Emy,X.0EmW,X.0EnW,X.0Emr,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0I(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0UNm,X.0tro,X.0pgh,X.0EnR,X.0EmW,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0J(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0UNm,X.0uln,X.0trm,X.0pgh,X.0EnW,X.0EnT,X.0EmY,X.0EnV,java.lang.CharSequence)void`
- `X.0pgh#A0d(java.util.List)void`
- `X.0pgh#A0t(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Fy,X.0fat,X.0UNm,X.0uln,X.0trm,X.0Qni,X.0EnW,X.0EnT,X.0EmY,X.0EnV,X.0EnU)void`

Log:
```
   [StoryLinksHook.install:70] (IE|StoryLinks) ✅ link stickers via hashed accessor
   [StoryLinksHook.installButtonHook:155] (IE|StoryLinks) ✅ button hook on 3 builder(s)
   [StoryLinksHook.installClickHook:201] (IE|StoryLinks) ✅ click hook installed
```

## OK — StoryInfoHook

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryInfoHook().install(dexKitBridge, lpparam.classLoader)` (1525 ms)

Hooked (11):
- `X.0pgh#A0q(X.07pW,X.0pgh,X.0HWw,boolean)java.lang.CharSequence[]`
- `X.0pgh#A0r(X.0pgh)java.lang.CharSequence[]`
- `X.0pgh#A0s(X.0pgh,boolean)java.lang.CharSequence[]`
- `X.0okZ#A01(android.content.Context,X.0okZ)void`
- `X.0okZ#A03(X.0okZ,java.lang.CharSequence)void`
- `X.0pgh#A0F(android.content.Context,X.0fhT,com.instagram.model.reels.ReelItem,X.0pgh,X.0EmY)void`
- `X.0pgh#A0G(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0tpp,X.0tql,X.0UNm,X.0trl,X.0tro,X.0pgh,X.0Emy,X.0EmW,X.0EnW,X.0Emr,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0I(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0UNm,X.0tro,X.0pgh,X.0EnR,X.0EmW,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0J(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0UNm,X.0uln,X.0trm,X.0pgh,X.0EnW,X.0EnT,X.0EmY,X.0EnV,java.lang.CharSequence)void`
- `X.0pgh#A0d(java.util.List)void`
- `X.0pgh#A0t(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Fy,X.0fat,X.0UNm,X.0uln,X.0trm,X.0Qni,X.0EnW,X.0EnT,X.0EmY,X.0EnV,X.0EnU)void`

Log:
```
   [StoryInfoHook.install:87] (IE|StoryInfo) ✅ Media fields via hashed accessor
   [StoryInfoHook.installButtonHook:187] (IE|StoryInfo) ✅ button hook on 3 builder(s)
   [StoryInfoHook.installClickHook:240] (IE|StoryInfo) ✅ click hook installed
```

## OK — CommentCopyHook

`new CommentCopyHook().install(dexKitBridge, lpparam.classLoader)` (314 ms)

Hooked (4):
- `X.0UXU#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#GJb(java.lang.String,java.lang.String,float,boolean)void`

Log:
```
   [CommentCopyHook.findAndHookNew:200] (InstaEclipse | CopyComment): ✅ Hooked com.instagram.comments.mvvm.viewmodel.CommentListViewModel.GJb (resolver=X.020q.A01)
```

## OK — CaptionCopyContextMenuHook

`new CaptionCopyContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (1416 ms)

Hooked (11):
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `X.0PVY#A00(X.05zu,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PVY,X.0FrB,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.08Nw#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.08Nw#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0PsY#A0A(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0PBv#A01(boolean)java.util.List`
- `X.08Nn#A06(X.0LCC,X.03y5,X.08Nn)java.util.ArrayList`
- `X.08OG#A01(com.instagram.common.session.UserSession,com.instagram.feed.media.mediaoption.MediaOption$Option,boolean)int`
- `X.0fhT#A00(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,X.0dzc,X.0lgw,java.lang.Integer,java.lang.String,java.lang.String,java.lang.String,float,boolean)void`
- `X.0fhT#A01(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,java.lang.String,java.lang.String,float,int,int,boolean,boolean,boolean,boolean,boolean)void`

Log:
```
   [CaptionCopyContextMenuHook.resolveCaptionGetter:231] (IE|Caption) ✅ caption via hashed accessor
   [CaptionCopyContextMenuHook.installAddButtonHook:523] (IE|Caption) ✅ Caption copy menu hook installed
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:791] (IE|Caption) ✅ reel label override on X.0fhT.A00
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:791] (IE|Caption) ✅ reel label override on X.0fhT.A01
```

## OK — DisableDoubleTapLikeHook

`new DisableDoubleTapLikeHook().install(dexKitBridge, lpparam.classLoader)` (875 ms)

Hooked (2):
- `X.0PSG#A02(android.app.Activity,android.content.Context,X.03vF,com.instagram.common.session.UserSession,X.0UQm,com.instagram.feed.media.Media,X.00f2,X.0JBZ,X.0CaD,X.0CaW,com.instagram.search.common.analytics.SearchContext,X.0Aim,java.lang.Double,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,int,int,int,boolean)void`
- `X.0JMl#invoke(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [DisableDoubleTapLikeHook.findAndHook:139] (InstaEclipse | DoubleTapLike): Feed hooked on X.0PSG.A02
   [DisableDoubleTapLikeHook.findAndHook:161] (InstaEclipse | DoubleTapLike): Reels like action hooked on X.0JMl.invoke
```

## OK — DoubleTapExtrasHook

`new io.github.somilkhan.instaeclipse2.mods.misc.DoubleTapExtrasHook().install(dexKitBridge, lpparam.classLoader)` (337 ms)

Hooked (6):
- `X.025i#onDoubleTap(android.view.MotionEvent)boolean`
- `X.026C#onDoubleTap(android.view.MotionEvent)boolean`
- `X.0Xb8#FQ4()void`
- `X.0akz#Fqy(X.03XB,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.0alP#Fqy(X.03XB,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.0A3J#A00(X.0Mfx,X.03XB,X.02pv,java.lang.String,java.lang.String,java.util.List,boolean)void`

Log:
```
   [DoubleTapExtrasHook.install:101] (InstaEclipse | DoubleTapLike): ✅ comments 2, DM senders 3, DM double-tap 1
```

## OK — FeedPhotoZoomHook

`new FeedPhotoZoomHook().install(lpparam.classLoader)` (55 ms)

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

`new IgThemeHook().install(hostClassLoader)` (65 ms)

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
   [IgThemeHook.install:57] (InstaEclipse | Theme): hooks installed enabled=true
```

## OK — ForceReelQualityHook

`new ForceReelQualityHook().install(dexKitBridge, lpparam.classLoader)` (106 ms)

Hooked (11):
- `com.instagram.feed.media.Media#BOs(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#CkM(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#CkO(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#CkP(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#CkQ(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#CkR(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#DA4(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#DA5(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#DA7(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#DA9(int)com.google.common.collect.ImmutableList`
- `com.instagram.feed.media.Media#DAA(int)com.google.common.collect.ImmutableList`

Log:
```
   [ForceReelQualityHook.installHashed:198] (InstaEclipse | ForceReelQuality): ✅ Hooked com.instagram.feed.media.Media list accessors x11 [video_versions hash] (height=C7R)
```

## OK — MaxUploadQualityHook

`new io.github.somilkhan.instaeclipse2.mods.media.MaxUploadQualityHook().install(dexKitBridge, lpparam.classLoader)` (492 ms)

Hooked (1):
- `X.0BvI#A00(X.02pv)boolean`

Log:
```
   [MaxUploadQualityHook.install:73] (IE|MaxUploadQuality) ✅ hooked 1 method(s)
```

## OK — DisableVideoAutoPlayHook

`new DisableVideoAutoPlayHook().handleAutoPlayDisable(dexKitBridge)` (187 ms)

Hooked (9):
- `android.content.res.Resources#getDrawable(int)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawable(int,android.content.res.Resources$Theme)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawableForDensity(int,int,android.content.res.Resources$Theme)android.graphics.drawable.Drawable`
- `android.content.res.Resources#getDrawableForDensity(int,int)android.graphics.drawable.Drawable`
- `android.widget.ImageView#setImageResource(int)void`
- `android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)void`
- `android.view.View#performClick()boolean`
- `android.view.View#callOnClick()boolean`
- `X.00wD#A00(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DisableVideoAutoPlayHook.hookManualPlayOverlayLifecycle:121] (InstaEclipse | AutoPlayDisable): Hooked manual play overlay lifecycle.
   [DisableVideoAutoPlayHook.hookMethod:94] (InstaEclipse | AutoPlayDisable): ✅ Hooked (dynamic check): X.00wD.A00
```

## OK — BuildExpiredPopupHook

`new BuildExpiredPopupHook().install(dexKitBridge, lpparam.classLoader)` (301 ms)

Hooked (2):
- `X.03Ai#A02(androidx.fragment.app.FragmentActivity,X.0Mlg)void`
- `X.03Ai#A04()boolean`

Log:
```
   [BuildExpiredPopupHook.install:86] (IE|BuildExpired) ✅ hooked show-popup → X.03Ai.A02
   [BuildExpiredPopupHook.install:109] (IE|BuildExpired) ✅ hooked snooze-check → X.03Ai.A04
```

## NO-HOOK — FeedVideoDownloadHook

`new FeedVideoDownloadHook().install(lpparam.classLoader)` (58 ms)

Log:
```
   [FeedVideoDownloadHook.install:190] (IE|DL) media model: mutable=false liveTree=false listCandidates=0
```

## OK — FeedVideoDownloadHook

`FeedVideoDownloadHook.installVideoUrlCaptureHook(dexKitBridge, lpparam.classLoader)` (2256 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoVideoVersion#getUrl()java.lang.String`
- `com.instagram.api.schemas.VideoVersion#getUrl()java.lang.String`

Log:
```
   [SelectedMediaAdapter.install:63] (IE|MediaAdapter) resolved properties=5 manifest=false direct=false story=true
   [FeedVideoDownloadHook.discoverDynamicMediaModel:1309] (IE|DL|DexKit) dynamic media dict=X.06sb
   [FeedVideoDownloadHook.resolveVideoVersionsGetters:1365] (IE|DL|DexKit) video_versions getters=1
   [FeedVideoDownloadHook.resolveCarouselGetter:1410] (IE|DL|DexKit) carousel getter=hash(carousel_media)
   [FeedVideoDownloadHook.resolveDashManifestGetter:1483] (IE|DL|DexKit) dash manifest getter=hash(video_dash_manifest)
   [FeedVideoDownloadHook.resolveHasAudioGetter:1437] (IE|DL|DexKit) has_audio getter=hash(has_audio)
   [FeedVideoDownloadHook.resolveIsVideoMethod:1851] (IE|DL|DexKit) isVideo: no Media ()Z getter (457+ layout), using video_versions presence
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1235] (IE|DL|DexKit) VideoVersionIntf implementors found: 2
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1251] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.ImmutablePandoVideoVersion
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1251] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.VideoVersion
   [FeedVideoDownloadHook.resolveUsernameGetter:1898] (IE|DL|Username) userClass=X.02gr
   [FeedVideoDownloadHook.resolveUsernameGetter:1943] (IE|DL|Username) ✅ username via hashed accessor on X.02gr
   [FeedVideoDownloadHook.resolveMediaAuthorGetter:1989] (IE|DL|Username) ✅ media author via hash(user)
   [FeedVideoDownloadHook.resolveDictUserGetter:2067] (IE|DL|Username) dictUserGetter: not needed (458+ layout), using hash(user)
```

## OK — PostDownloadContextMenuHook

`new PostDownloadContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (414 ms)

Hooked (5):
- `X.0PVY#A00(X.05zu,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PVY,X.0FrB,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.08Nw#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.08Nw#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0PsY#A0A(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0PBv#A01(boolean)java.util.List`

Log:
```
   [PostDownloadContextMenuHook.installAddButtonHook:338] (IE|Post) ✅ Post menu inject hook installed
   [PostDownloadContextMenuHook.resolveReelClickDispatcher:381] (IE|Post) selected Reel dispatcher bound=false
   [PostDownloadContextMenuHook.installAllowlistPatchHook:535] (IE|Post) ✅ Allowlist patch hooked: X.0PBv.A01
```

## OK — InstantSaveHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantSaveHook().install(lpparam.classLoader)` (53 ms)

Hooked (2):
- `com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1#<init>(androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,X.0lun,com.instagram.common.session.UserSession,com.instagram.compose.ui.emitter.AnimatedParticleEffectState,X.02Ot,java.util.List,java.util.List,X.0Mow,int,boolean)`
- `com.instagram.quicksnap.viewer.compose.PointerTouchKt$handleTapAndLongPressWithRelease$2$1$1$1#<init>(X.0Mow,kotlin.jvm.functions.Function0,kotlin.jvm.functions.Function1,kotlin.jvm.functions.Function1,float,long,boolean)`

Log:
```
   [InstantSaveHook.cacheCurrentInstant:77] (IE|InstantSave) ✅ viewer cache hooked: com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1
   [InstantSaveHook.wrapLongPress:183] (IE|InstantSave) ✅ long-press hooked
```

## OK — InstantUploadHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantUploadHook().install(lpparam.classLoader)` (56 ms)

Hooked (10):
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1C(android.content.Context,android.graphics.Bitmap,X.07eq,X.0HU8,X.0IRb,X.0VKm,java.lang.Long,java.lang.String,java.lang.String,X.0Mow,X.02rd,X.0Mzx,float,long,boolean)java.lang.Object`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A07()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1G()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1H()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1I()void`
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1A()void`
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

`new GhostEphemeralKeepHook().install(dexKitBridge, lpparam.classLoader)` (451 ms)

Hooked (7):
- `X.05s9#A0c(boolean)boolean`
- `X.05s9#A0a()boolean`
- `X.06dw#H84(X.02tm,X.0faP,X.0FeC)void`
- `X.03Wv#unsafeParseFromJson(X.06d9)java.lang.Object`
- `X.05oF#unsafeParseFromJson(X.06d9)java.lang.Object`
- `X.03Wv#parseFromJsonParser(X.06d9)java.lang.Object`
- `X.05oF#parseFromJsonParser(X.06d9)java.lang.Object`

Log:
```
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.05s9.A0c
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.05s9.A0a
   [GhostEphemeralKeepHook.hookServerPing:187] (IE|Ephemeral) ✅ server-ping hook → X.06dw.H84
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.03Wv.unsafeParseFromJson
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.05oF.unsafeParseFromJson
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.03Wv.parseFromJsonParser
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.05oF.parseFromJsonParser
```

## OK — GhostPermanentViewHook

`new GhostPermanentViewHook().install(dexKitBridge, lpparam.classLoader)` (186 ms)

Hooked (1):
- `X.03ZB#unsafeParseFromJson(X.06d9)java.lang.Object`

Log:
```
   [GhostPermanentViewHook.install:96] (IE|ViewOnceMedia) ✅ hooking X.03ZB.unsafeParseFromJson
   [GhostPermanentViewHook.install:103] (IE|ViewOnceMedia) ✅ hooked
```

## OK — ViewOnceBadgeHook

`new ViewOnceBadgeHook().install(dexKitBridge, lpparam.classLoader)` (81 ms)

Hooked (1):
- `X.0AHJ#<init>(X.07Ko,X.09SH,X.0AHA,com.instagram.direct.thread.analytics.DirectThreadAnalyticsParams,com.instagram.direct.wellbeing.harmfulcontent.ondevicenudity.view.data.PrivacyMediaOverlayViewModel,X.0GxM,java.lang.String,java.lang.String,boolean,boolean,boolean,boolean)`

Log:
```
   [ViewOnceBadgeHook.install:75] (IE|VOBadge) ✅ hooked X.0AHJ.<init>
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

`new io.github.somilkhan.instaeclipse2.mods.ui.SavedOnProfileHook().install(dexKitBridge, hostClassLoader)` (59 ms)

Hooked (1):
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [SavedOnProfileHook.install:128] (IE|SavedProfile) ✅ X.0W0i.A00 / public static X.08r4 X.01N1.A0T(androidx.fragment.app.FragmentActivity,X.01zc)
```

## OK — HideHeaderButtonsHook

`new io.github.somilkhan.instaeclipse2.mods.ui.HideHeaderButtonsHook().install(hostClassLoader)` (51 ms)

Hooked (1):
- `android.app.Activity#onResume()void`

Log:
```
   [HideHeaderButtonsHook.install:46] (IE|HideButtons) ✅ installed
```

## OK — ShareLinkDownloadHook

`new io.github.somilkhan.instaeclipse2.mods.media.ShareLinkDownloadHook().install(hostClassLoader)` (53 ms)

Hooked (3):
- `com.instagram.common.session.UserSession#<init>(X.06t2,java.lang.String,X.02dc,boolean,boolean)`
- `android.app.Activity#onCreate(android.os.Bundle)void`
- `android.app.Activity#onNewIntent(android.content.Intent)void`

Log:
```
   [ShareLinkDownloadHook.install:71] (IE|ShareDL) ✅ installed
```

## OK — StoryDownloadHook

`new StoryDownloadHook().install(dexKitBridge, lpparam.classLoader)` (1047 ms)

Hooked (16):
- `X.0pgh#A0q(X.07pW,X.0pgh,X.0HWw,boolean)java.lang.CharSequence[]`
- `X.0pgh#A0r(X.0pgh)java.lang.CharSequence[]`
- `X.0pgh#A0s(X.0pgh,boolean)java.lang.CharSequence[]`
- `X.0pgh#A06(X.0pgh)java.util.ArrayList`
- `X.0pgh#A07(X.0pgh,boolean)java.util.ArrayList`
- `X.0okZ#A01(android.content.Context,X.0okZ)void`
- `X.0okZ#A03(X.0okZ,java.lang.CharSequence)void`
- `X.0pgh#A0F(android.content.Context,X.0fhT,com.instagram.model.reels.ReelItem,X.0pgh,X.0EmY)void`
- `X.0pgh#A0G(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0tpp,X.0tql,X.0UNm,X.0trl,X.0tro,X.0pgh,X.0Emy,X.0EmW,X.0EnW,X.0Emr,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0I(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0T2m,X.0UNm,X.0tro,X.0pgh,X.0EnR,X.0EmW,X.0EmY,java.lang.CharSequence)void`
- `X.0pgh#A0J(android.content.DialogInterface$OnDismissListener,X.08Fy,X.0UNm,X.0uln,X.0trm,X.0pgh,X.0EnW,X.0EnT,X.0EmY,X.0EnV,java.lang.CharSequence)void`
- `X.0pgh#A0d(java.util.List)void`
- `X.0pgh#A0t(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.08Fy,X.0fat,X.0UNm,X.0uln,X.0trm,X.0Qni,X.0EnW,X.0EnT,X.0EmY,X.0EnV,X.0EnU)void`
- `X.0olR#A0J(android.view.View,X.0orf,com.instagram.model.reels.ReelItem,java.lang.Integer)void`
- `X.0RDk#A00()java.util.List`
- `instagram.features.stories.fragment.ReelViewerFragment#FLz(com.instagram.model.reels.ReelItem,X.0Sam)void`

Log:
```
   [StoryDownloadHook.install:194] (IE|Highlight) reel class X.04od
   [StoryDownloadHook.installButtonInjectorHook:601] (IE|Story) button injector hooked 3 builder(s)
   [StoryDownloadHook.installHighlightButtonInjectorHook:666] (IE|Story) Highlight injector hooked 2 builder(s)
   [StoryDownloadHook.installClickHandlerHook:800] (IE|Story) click handler hooked 8 dispatcher(s)
   [StoryDownloadHook.installHighlightTrayHook:292] (IE|HighlightTray) ✅ X.0olR legacy=true popup=false loader=false
   [StoryDownloadHook.installStoryCaptureHook:450] (IE|StoryCache) capture hook: 1 method(s)
```

## OK — ReelDownloadHook

`new ReelDownloadHook().install(dexKitBridge, lpparam.classLoader)` (480 ms)

Hooked (5):
- `X.08Nn#A06(X.0LCC,X.03y5,X.08Nn)java.util.ArrayList`
- `X.0CHt#A07(X.0CHw,com.instagram.common.session.UserSession)boolean`
- `X.08Nn#A0B(android.graphics.PointF,android.view.View,android.view.View,X.03y5,X.092a,X.0dqY,X.08Nn)void`
- `X.08Nn#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0RDk)void`
- `X.08Nn#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0RDk)void`

Log:
```
   [ReelDownloadHook.installGateHook:502] (IE|Reel) gate config 36313978552585585 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installGateHook:502] (IE|Reel) gate config 36313978552847731 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installReduceOptionsListPatch:240] (IE|Reel) ✅ Options-list patch hooked: X.08Nn.A06
   [ReelDownloadHook.install:81] (IE|Reel) string matches=4 candidateClasses=3
   [ReelDownloadHook.installEligibilityGate457:419] (IE|Reel) ✅ 457 eligibility gate: X.0CHt.A07
   [ReelDownloadHook.installSheetRowInjection:363] (IE|Reel) ✅ sheet row injection: X.08Nn builders=1 rowAdders=2
```

## OK — ProfilePicDownloadHook

`ProfilePicDownloadHook.install(dexKitBridge, hostClassLoader)` (67 ms)

Hooked (2):
- `androidx.fragment.app.Fragment#onResume()void`
- `android.view.View#onAttachedToWindow()void`

Log:
```
   [ProfilePicDownloadHook.resolveHdGetter:118] (IE|ProfileDL) ✅ HD url via X.02gr hash(hd_profile_pic_url_info)
```

## OK — interceptor

`interceptor.handleInterceptor(lpparam)` (68 ms)

Hooked (2):
- `com.instagram.api.tigon.TigonServiceLayer#startRequest(X.03rt,X.03sf,X.03tw)X.0Myb`
- `X.01zq#startRequest(X.03rt,X.03sf,X.03tw)X.0Myb`

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

