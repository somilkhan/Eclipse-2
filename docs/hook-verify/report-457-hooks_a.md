# hook-verify report

APK: `ig457.apk`

Status legend: **OK** = hooks installed and no failure lines; **PARTIAL** = some hooks installed but at least one sub-lookup logged a failure; **FAIL** = failure logged and nothing hooked; **NO-HOOK** = nothing hooked and nothing logged (usually a hook that installs lazily at runtime, or one that only hooks framework classes once an Activity exists); **ERROR** = install threw.

```
OK       instagramUI                      hooks=5      409 ms
PARTIAL  DevOptionsUnlockHook             hooks=1      916 ms
OK       GhostDMSeenHook                  hooks=1      165 ms
OK       GhostDMMarkAsReadHook            hooks=1       51 ms
OK       GhostChannelMarkAsReadHook       hooks=1       51 ms
OK       GhostTypingIndicatorHook         hooks=1      168 ms
OK       GhostScreenshotDetectionHook     hooks=1      150 ms
OK       ScreenshotPermissionHook         hooks=2       53 ms
OK       GhostViewOnceHook                hooks=1      172 ms
OK       GhostStorySeenHook               hooks=6      164 ms
PARTIAL  KeepUnsentMessagesHook           hooks=5     1001 ms
OK       UnsentThreadButtonHook           hooks=5       57 ms
OK       HideChatsHook                    hooks=2      286 ms
OK       CustomFontHook                   hooks=9       73 ms
OK       metaAi                           hooks=2       51 ms
OK       metaAi                           hooks=8     1734 ms
OK       DisableRepostHook                hooks=2      409 ms
OK       LockDirectMessagesHook           hooks=1       52 ms
OK       HideSuggestedFeedItemsHook       hooks=2      415 ms
OK       AdBlocker                        hooks=1      169 ms
OK       TrackingLinkDisable              hooks=5       55 ms
OK       ExactTimestampHook               hooks=1      134 ms
OK       ExpandCaptionsHook               hooks=2      182 ms
OK       HideCommunityNotesHook           hooks=2       68 ms
OK       ReelsControlsHook                hooks=3      854 ms
FAIL     AccountLimitHook                 hooks=0      226 ms
OK       AirplaneModeHook                 hooks=3       54 ms
FAIL     HighResolutionImageHook          hooks=0      111 ms
OK       OnboardingPromptHook             hooks=1      175 ms
NO-HOOK  NotificationRegistrationHook     hooks=0       51 ms
OK       StartupTabHook                   hooks=4       52 ms
OK       SwipeToCameraHook                hooks=1       52 ms
OK       FollowingOnlyFeedHook            hooks=5      164 ms
OK       ProfileFollowsYouHook            hooks=2      138 ms
OK       PlaybackSpeedHook                hooks=23    1215 ms
FAIL     HideNotesTrayHook                hooks=0      280 ms
PARTIAL  VoiceMessageDownloadHook         hooks=1      284 ms
FAIL     EditHistoryHook                  hooks=0      242 ms
NO-HOOK  JumpToFirstMessageHook           hooks=0       51 ms
NO-HOOK  ReadReceiptChatHook              hooks=0       53 ms
OK       StoryViewerSearchHook            hooks=1      284 ms
OK       CommentSearchHook                hooks=2      469 ms
OK       ChatExportHook                   hooks=1      342 ms
FAIL     FollowListExportHook             hooks=0      198 ms
OK       LikeUndoHook                     hooks=1      288 ms
OK       ShareSheetCleanupHook            hooks=2       56 ms
NO-HOOK  ExternalBrowserHook              hooks=0       51 ms
OK       DisableStoryFlippingHook         hooks=1       87 ms
PARTIAL  StoryMentionHook                 hooks=9      515 ms
OK       StoryLinksHook                   hooks=9      356 ms
OK       StoryInfoHook                    hooks=9     1487 ms
OK       CommentCopyHook                  hooks=4      235 ms
PARTIAL  CaptionCopyContextMenuHook       hooks=6      295 ms
OK       DisableDoubleTapLikeHook         hooks=3      779 ms
OK       DoubleTapExtrasHook              hooks=6      291 ms
OK       FeedPhotoZoomHook                hooks=1       53 ms
FAIL     LocationSpoofHook                hooks=0       52 ms
OK       IgThemeHook                      hooks=21      63 ms
OK       ForceReelQualityHook             hooks=1      139 ms
FAIL     MaxUploadQualityHook             hooks=0      161 ms
OK       DisableVideoAutoPlayHook         hooks=9      169 ms
OK       BuildExpiredPopupHook            hooks=2      279 ms
NO-HOOK  FeedVideoDownloadHook            hooks=0       59 ms
FAIL     FeedVideoDownloadHook            hooks=0     2066 ms
PARTIAL  PostDownloadContextMenuHook      hooks=4      363 ms
OK       InstantSaveHook                  hooks=2       63 ms
PARTIAL  InstantUploadHook                hooks=9       57 ms
PARTIAL  GhostEphemeralKeepHook           hooks=5      397 ms
OK       GhostPermanentViewHook           hooks=1      169 ms
OK       ViewOnceBadgeHook                hooks=1       77 ms
OK       LockedCollectionsHook            hooks=1       52 ms
FAIL     SavedOnProfileHook               hooks=0       51 ms
OK       HideHeaderButtonsHook            hooks=1       51 ms
OK       ShareLinkDownloadHook            hooks=3       52 ms
OK       StoryDownloadHook                hooks=14     978 ms
OK       ReelDownloadHook                 hooks=5      466 ms
OK       ProfilePicDownloadHook           hooks=2       69 ms
OK       interceptor                      hooks=2       66 ms
OK       TerminatedExecutorGuard          hooks=1       51 ms
TOTAL: 79 steps  OK=57 PARTIAL=8 FAIL=9 NO-HOOK=5 ERROR=0
```

## OK — instagramUI

`instagramUI.mainActivity(hostClassLoader)` (409 ms)

Hooked (5):
- `com.instagram.base.activity.BaseFragmentActivity#onCreate(android.os.Bundle)void`
- `com.instagram.base.activity.IgFragmentActivity#onResume()void`
- `com.instagram.mainactivity.InstagramMainActivity#BOQ()X.04hD`
- `android.view.View#performLongClick()boolean`
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [UIHookManager.mainActivity:184] (InstaEclipse): ✅ main onCreate hooked via com.instagram.base.activity.BaseFragmentActivity#onCreate
   [UIHookManager.mainActivity:201] (InstaEclipse): ✅ main onResume hooked via com.instagram.base.activity.IgFragmentActivity#onResume
   [BottomSheetHookUtil.hookMethod:75] (InstaEclipse | BottomSheet): ✅ Hooked: com.instagram.mainactivity.InstagramMainActivity.BOQ
```

## PARTIAL — DevOptionsUnlockHook

`new DevOptionsUnlockHook().handleDevOptions(dexKitBridge)` (916 ms)

Hooked (1):
- `X.02ua#A01(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DevOptionsUnlockHook.findAndHookDynamicMethod:73] (InstaEclipse | DevOptionsEnable): 🔍 Discovery Tier 1 (String)...
!! [DevOptionsUnlockHook.findAndHookDynamicMethod:105] (InstaEclipse | DevOptionsEnable): ⚠️ Tier 1 failed. Discovery Tier 2 (Structural)...
   [DevOptionsUnlockHook.hookExactMethod:224] (InstaEclipse | DevOptionsEnable): ✅ Hooked: X.02ua.A01
   [DevOptionsUnlockHook.findAndHookDynamicMethod:112] (InstaEclipse | DevOptionsEnable): 🎯 Found via structural match: X.02ua.A01
```

## OK — GhostDMSeenHook

`new GhostDMSeenHook().handleSeenBlock(dexKitBridge)` (165 ms)

Hooked (1):
- `X.0Atf#A0A(com.instagram.common.session.UserSession,X.02wI,java.lang.String,java.lang.String,java.lang.String)void`

Log:
```
   [GhostDMSeenHook.handleSeenBlock:86] (InstaEclipse | GhostModeSeen): ✅ Hooked: X.0Atf.A0A
```

## OK — GhostDMMarkAsReadHook

`new GhostDMMarkAsReadHook(moduleSourceDir).install(lpparam.classLoader)` (51 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostChannelMarkAsReadHook

`new GhostChannelMarkAsReadHook().install(lpparam.classLoader)` (51 ms)

Hooked (1):
- `android.view.View#onAttachedToWindow()void`

## OK — GhostTypingIndicatorHook

`new GhostTypingIndicatorHook().handleTypingBlock(dexKitBridge)` (168 ms)

Hooked (1):
- `X.06Zw#A0S(boolean)void`

Log:
```
   [GhostTypingIndicatorHook.handleTypingBlock:86] (InstaEclipse | TypingBlock): ✅ Hooked (dynamic check): X.06Zw.A0S
```

## OK — GhostScreenshotDetectionHook

`new GhostScreenshotDetectionHook().handleScreenshotBlock(dexKitBridge)` (150 ms)

Hooked (1):
- `X.07WF#GKZ(long)void`

Log:
```
   [GhostScreenshotDetectionHook.handleScreenshotBlock:74] (InstaEclipse | ScreenshotBlock): ✅ Hooked (dynamic check): X.07WF.GKZ
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

`new GhostViewOnceHook().handleViewOnceBlock(dexKitBridge)` (172 ms)

Hooked (1):
- `X.05tf#HCv(X.02sx,X.0XoP,X.0ArI)void`

Log:
```
   [GhostViewOnceHook.tryHookViewOnce:77] (InstaEclipse | ViewOnce): ✅ Hooked (endpoint+raven_media): X.05tf.HCv
```

## OK — GhostStorySeenHook

`new GhostStorySeenHook().handleStorySeenBlock(dexKitBridge)` (164 ms)

Hooked (6):
- `X.019u#A0R(java.lang.String,java.lang.Object)void`
- `X.019u#A0E(java.lang.String,java.lang.Object)void`
- `X.019u#A0F(java.util.Map)void`
- `X.02pM#A0K(java.lang.Object)void`
- `X.02pM#A0S(X.02pN)void`
- `X.02pM#A0T(X.02pN)void`

Log:
```
   [GhostStorySeenHook.tryModernBatchBlock:103] (InstaEclipse | StoryBlock): ✅ Hooked 6 sender(s) on store X.02pM (item builder: X.02pN.A06)
```

## PARTIAL — KeepUnsentMessagesHook

`new KeepUnsentMessagesHook().install(dexKitBridge, lpparam.classLoader)` (1001 ms)

Hooked (5):
- `androidx.fragment.app.Fragment#performResume()void`
- `androidx.fragment.app.Fragment#performPause()void`
- `androidx.fragment.app.Fragment#performDestroyView()void`
- `androidx.fragment.app.Fragment#onResume()void`
- `androidx.fragment.app.Fragment#onPause()void`

Log:
```
   [CurrentConversation.install:81] (IE|Conversation) lifecycle dispatchers resolved=3
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=anchors candidates=1 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=remove-shape candidates=5 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=remove-invokes candidates=31 matches=25
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=entry-call candidates=25 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=ownership-call candidates=25 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=ownership-definition candidates=1 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=sender-field candidates=4 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=id-getter candidates=2 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=client-getter candidates=3 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=id-lookup candidates=38 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=client-lookup candidates=38 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=store-session candidates=1 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=entry-session candidates=1 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=lookup-lists candidates=2 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=lookup-comparator candidates=2 matches=1
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=comparator-value candidates=1 matches=0
   [UnsentMessageResolver.diagnostic:155] (IE|UnsentBinding-v1) stage=resolved candidates=1 matches=0
   [KeepUnsentMessagesHook.install:92] (IE|KeepUnsent) sender binding=false; optional in-chat binding=false
   [KeepUnsentMessagesHook.resolveEntryClass:391] (IE|KeepUnsent) ✅ thread entry → X.03GI
!! [KeepUnsentMessagesHook.hookRemovePrimitive:297] (IE|KeepUnsent) ❌ remove primitive not found
   [CurrentThreadTracker.install:95] (IE|ThreadTracker) ✅ Fragment onResume/onPause hooked
   [KeepUnsentMessagesHook.hookCurrentThread:134] (IE|KeepUnsent) current-thread tracker hooked 0 method(s)
```

## OK — UnsentThreadButtonHook

`new ps.reso.instaeclipse.mods.ghost.UnsentThreadButtonHook().install(lpparam.classLoader)` (57 ms)

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

`new ps.reso.instaeclipse.mods.ghost.HideChatsHook().install(dexKitBridge, lpparam.classLoader)` (286 ms)

Hooked (2):
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0d(X.00EF,X.0HjN,java.util.Comparator,java.util.List,boolean)java.util.ArrayList`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0e(java.util.List)java.util.ArrayList`

Log:
```
   [HiddenInboxFilter.install:93] (IE|HideChats) inbox methods resolved=2; execution requires a real inbox
```

## OK — CustomFontHook

`new ps.reso.instaeclipse.mods.ui.CustomFontHook().install(lpparam.classLoader)` (73 ms)

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

`metaAi.install(lpparam.classLoader)` (51 ms)

Hooked (2):
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup)android.view.View`

Log:
```
   [RemoveMetaAIHook.install:126] (IE|RemoveMetaAI) ✅ installed
```

## OK — metaAi

`metaAi.installReels(dexKitBridge, lpparam.classLoader)` (1734 ms)

Hooked (8):
- `X.0cnG#A01(boolean)java.util.List`
- `X.0Ckr#A06(X.0BX4,X.075A,X.0Ckr)java.util.ArrayList`
- `X.0Ckv#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Ckv#A0c(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0Zwh,X.0Knm)void`
- `X.0Ckr#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PHJ)void`
- `X.0Ckv#A09(X.0BX4,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0Zwh,X.0Ckv)void`
- `X.0Ckr#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PHJ)void`
- `X.0Ckv#A0d(android.content.Context,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0Zwh,X.0Knm,java.lang.Integer,java.lang.String)void`

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

`new ps.reso.instaeclipse.mods.ui.DisableRepostHook().install(dexKitBridge, lpparam.classLoader)` (409 ms)

Hooked (2):
- `X.0Mnv#EF5(android.view.View,X.010B,X.0Gv1,java.lang.String,java.lang.String,java.lang.String,kotlin.jvm.functions.Function0,int)void`
- `X.0Agr#A0R(X.0Ktu,kotlin.jvm.functions.Function1)void`

Log:
```
   [DisableRepostHook.installFeedCommit:156] (IE|Repost) feed postRepost launcher: 1 method(s)
   [DisableRepostHook.install:73] (IE|Repost) ✅ installed: 2 method(s)
```

## OK — LockDirectMessagesHook

`new ps.reso.instaeclipse.mods.ui.LockDirectMessagesHook().install(lpparam.classLoader)` (52 ms)

Hooked (1):
- `com.instagram.modal.ModalActivity#onResume()void`

Log:
```
   [LockDirectMessagesHook.install:61] (IE|LockDMs) ✅ installed
```

## OK — HideSuggestedFeedItemsHook

`new HideSuggestedFeedItemsHook().install(dexKitBridge, hostClassLoader)` (415 ms)

Hooked (2):
- `X.02AQ#unsafeParseFromJson(X.088A)java.lang.Object`
- `X.02AQ#parseFromJsonParser(X.088A)java.lang.Object`

Log:
```
   [HideSuggestedFeedItemsHook.resolveHasLikedGetter:158] (InstaEclipse | HideLiked): ✅ has_liked getter com.instagram.feed.media.Media.A3t
   [HideSuggestedFeedItemsHook.install:130] (InstaEclipse | HideSuggested): ✅ Hooked: X.02AQ
```

## OK — AdBlocker

`new AdBlocker().disableSponsoredContent(dexKitBridge, hostClassLoader)` (169 ms)

Hooked (1):
- `X.06jF#A02(X.06jF,X.04Ib,X.05a3)boolean`

Log:
```
   [AdBlocker.disableSponsoredContent:68] (InstaEclipse | AdBlocker): ✅ Hooked (dynamic check, marker='Is ad pod'): X.06jF.A02
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

`new ps.reso.instaeclipse.mods.misc.ExactTimestampHook().install(dexKitBridge)` (134 ms)

Hooked (1):
- `X.03fs#A08(android.content.res.Resources,X.04TA,X.03fs,java.lang.Integer,double,double,boolean,boolean,boolean,boolean)java.lang.String`

Log:
```
   [ExactTimestampHook.install:84] (InstaEclipse | ExactTime): ✅ Hooked X.03fs.A08
```

## OK — ExpandCaptionsHook

`new ps.reso.instaeclipse.mods.feed.ExpandCaptionsHook().install(dexKitBridge)` (182 ms)

Hooked (2):
- `X.02oM#A03(android.content.Context,X.05i1,com.instagram.search.common.analytics.SearchContext,X.02oM,java.lang.String,java.lang.String,boolean)android.text.SpannableStringBuilder`
- `X.05k3#A00(X.02oW,java.lang.CharSequence,java.lang.CharSequence,java.lang.CharSequence,int,boolean)java.lang.CharSequence`

Log:
```
   [ExpandCaptionsHook.install:67] (InstaEclipse | ExpandCaptions): ✅ Hooked X.02oM.A03 → X.05k3.A00
```

## OK — HideCommunityNotesHook

`new ps.reso.instaeclipse.mods.feed.HideCommunityNotesHook().install(dexKitBridge, hostClassLoader)` (68 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoMediaOverlayPayloadSchema#Cqf()X.02ES`
- `com.instagram.api.schemas.MediaOverlayPayloadSchema#Cqf()X.02ES`

Log:
```
   [HideCommunityNotesHook.install:71] (InstaEclipse | CommunityNotes): ✅ notes getter Cqf hooked on 2 class(es)
```

## OK — ReelsControlsHook

`new ps.reso.instaeclipse.mods.extras.ReelsControlsHook().install(dexKitBridge, hostClassLoader)` (854 ms)

Hooked (3):
- `X.0AMH#A09(com.instagram.common.session.UserSession,boolean)boolean`
- `X.0CBZ#E59(int)android.view.View`
- `instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout#onInterceptTouchEvent(android.view.MotionEvent)boolean`

Log:
```
   [ReelsControlsHook.installAutoScroll:66] (InstaEclipse | Reels): ✅ auto-scroll gate X.0AMH.A09
   [ReelsControlsHook.installScrollLock:97] (InstaEclipse | Reels): ✅ pager X.0CBZ.A0H
```

## FAIL — AccountLimitHook

`new ps.reso.instaeclipse.mods.extras.AccountLimitHook().install(dexKitBridge, hostClassLoader)` (226 ms)

Log:
```
!! [ExtrasLookup.method:46] (InstaEclipse | Extras): Extras_AccountLimit not found in this Instagram version
```

## OK — AirplaneModeHook

`new ps.reso.instaeclipse.mods.extras.AirplaneModeHook().install(hostClassLoader)` (54 ms)

Hooked (3):
- `com.instagram.realtimeclient.RealtimeClientManager#<init>(android.content.Context,com.instagram.common.session.UserSession,com.instagram.realtimeclient.clientconfig.RealtimeClientConfig,com.instagram.realtimeclient.MainRealtimeEventHandler,X.0GeL)`
- `com.instagram.realtimeclient.RealtimeClientManager#startMqttClient()void`
- `com.instagram.realtimeclient.MainRealtimeEventHandler#onRealtimeEvent(java.lang.String,com.instagram.realtimeclient.RealtimeEvent)void`

Log:
```
   [AirplaneModeHook.install:84] (InstaEclipse | Airplane): ✅ hooked
```

## FAIL — HighResolutionImageHook

`new ps.reso.instaeclipse.mods.extras.HighResolutionImageHook().install(dexKitBridge, hostClassLoader)` (111 ms)

Log:
```
!! [HighResolutionImageHook.install:42] (InstaEclipse | HighResImages): ❌ image picker not found
```

## OK — OnboardingPromptHook

`new ps.reso.instaeclipse.mods.extras.OnboardingPromptHook().install(dexKitBridge, hostClassLoader)` (175 ms)

Hooked (1):
- `X.0dcp#A09(android.content.Context,com.instagram.bloks.hosting.IgBloksScreenConfig)void`

Log:
```
   [OnboardingPromptHook.install:77] (InstaEclipse | Onboarding): ✅ hooked 1 launcher(s)
```

## NO-HOOK — NotificationRegistrationHook

`new ps.reso.instaeclipse.mods.extras.NotificationRegistrationHook().install()` (51 ms)

## OK — StartupTabHook

`new ps.reso.instaeclipse.mods.ui.StartupTabHook().install(hostClassLoader)` (52 ms)

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

`new ps.reso.instaeclipse.mods.extras.SwipeToCameraHook().install(hostClassLoader)` (52 ms)

Hooked (1):
- `com.instagram.ui.swipenavigation.container.SwipeNavigationContainer#setInternalPosition(com.instagram.ui.swipenavigation.container.PositionConfig)void`

Log:
```
   [SwipeToCameraHook.install:69] (InstaEclipse | SwipeCamera): ✅ hooked
```

## OK — FollowingOnlyFeedHook

`new ps.reso.instaeclipse.mods.feed.FollowingOnlyFeedHook().install(dexKitBridge, hostClassLoader)` (164 ms)

Hooked (5):
- `X.03dn#A01(android.content.Context,X.08Ci,X.0TXl,com.instagram.common.session.UserSession,X.03dA,X.02vf)void`
- `X.08Ci#A0L(java.lang.String,java.lang.String)void`
- `X.08Ci#APe(java.lang.String,java.lang.String)void`
- `X.08Ci#APN(java.lang.String,java.lang.String)void`
- `X.08Ci#AO3(java.lang.String,java.lang.String)void`

Log:
```
   [FollowingOnlyFeedHook.install:129] (InstaEclipse | FollowingFeed): ✅ hooked X.03dn.A01 + 4 adders
```

## OK — ProfileFollowsYouHook

`new ps.reso.instaeclipse.mods.ui.ProfileFollowsYouHook().install(dexKitBridge, hostClassLoader)` (138 ms)

Hooked (2):
- `com.instagram.profile.fragment.UserDetailFragment#A0m(com.instagram.profile.fragment.UserDetailFragment,boolean)void`
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [ProfileFollowsYouHook.install:98] (InstaEclipse | FollowsYou): ✅ Hooked A0m / followed_by A2K
```

## OK — PlaybackSpeedHook

`new ps.reso.instaeclipse.mods.media.PlaybackSpeedHook().install(dexKitBridge, hostClassLoader)` (1215 ms)

Hooked (23):
- `X.06mS#A0O(com.instagram.feed.media.Media,X.0Kel,X.09tm,X.010B,X.0KcA,int,boolean)boolean`
- `X.06n0#Gj0(long,long)void`
- `X.0EqZ#HXv(float)void`
- `X.0Gc6#HXv(float)void`
- `X.0Gc6#<init>(android.content.Context,com.instagram.common.session.UserSession,X.00I2,X.01CE,X.0Msv)`
- `X.0Ixr#HXv(float)void`
- `X.0Ixr#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0Kel,X.0Nmi,X.0GdS,kotlin.jvm.functions.Function1,boolean,boolean)`
- `X.0Izr#HXv(float)void`
- `X.0Izr#<init>(android.content.Context,com.instagram.common.session.UserSession,X.00I2,X.01CE,X.0Msv)`
- `X.0S4O#HXv(float)void`
- `X.0S4O#<init>(android.content.Context,com.instagram.common.session.UserSession,X.0Kel,java.lang.String)`
- `X.06r1#A0D(X.06r1,float,float)void`
- `X.06r1#A02(X.06r1)void`
- `X.0HbX#A00(X.0Bb9,long,boolean)boolean`
- `android.app.Activity#dispatchTouchEvent(android.view.MotionEvent)boolean`
- `android.widget.TextView#setText(java.lang.CharSequence,android.widget.TextView$BufferType)void`
- `android.text.StaticLayout$Builder#obtain(java.lang.CharSequence,int,int,android.text.TextPaint,int)android.text.StaticLayout$Builder`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int)android.text.BoringLayout`
- `android.text.BoringLayout#make(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int)android.text.BoringLayout`

Log:
```
   [PlaybackSpeedHook.installFeed:462] (InstaEclipse | PlaybackSpeed): story/reel speed methods: 5
   [PlaybackSpeedHook.installFeed:463] (InstaEclipse | PlaybackSpeed): ✅ feed player X.06mS.A0O, started-cb X.06n0.Gj0, setters 1
   [PlaybackSpeedHook.installHero:219] (InstaEclipse | PlaybackSpeed): ✅ hero X.06r1.A0D, prepare A02
   [PlaybackSpeedHook.installReelScrubber:547] (InstaEclipse | PlaybackSpeed): ✅ reel scrubber gate X.0HbX.A00
   [PlaybackSpeedHook.installHoldSide:153] (InstaEclipse | PlaybackSpeed): ✅ hold side tracking
```

## FAIL — HideNotesTrayHook

`new ps.reso.instaeclipse.mods.ui.HideNotesTrayHook().install(dexKitBridge)` (280 ms)

Log:
```
!! [HideNotesTrayHook.install:67] (InstaEclipse | NotesTray): ❌ notes tray holder not found
```

## PARTIAL — VoiceMessageDownloadHook

`new ps.reso.instaeclipse.mods.media.VoiceMessageDownloadHook().install(dexKitBridge, hostClassLoader)` (284 ms)

Hooked (1):
- `X.0QCO#A00(X.0QCO,java.lang.String,java.lang.String,boolean)X.02wO`

Log:
```
!! [VoiceMessageDownloadHook.install:395] (IE|VoiceDL) ❌ java.lang.NullPointerException: Cannot read the array length because "<local8>" is null
```

## FAIL — EditHistoryHook

`new ps.reso.instaeclipse.mods.ghost.EditHistoryHook().install(dexKitBridge, hostClassLoader)` (242 ms)

Log:
```
!! [EditHistoryHook.install:91] (IE|EditHistory) ❌ getter=public final java.util.List X.02wO.A11() merge=null
```

## NO-HOOK — JumpToFirstMessageHook

`new ps.reso.instaeclipse.mods.ghost.JumpToFirstMessageHook().install()` (51 ms)

Log:
```
   [JumpToFirstMessageHook.install:51] (IE|JumpFirst) ✅ installed
```

## NO-HOOK — ReadReceiptChatHook

`new ps.reso.instaeclipse.mods.ghost.ReadReceiptChatHook().install(hostClassLoader)` (53 ms)

Log:
```
   [ReadReceiptChatHook.install:51] (IE|ReadReceiptChat) ✅ installed
```

## OK — StoryViewerSearchHook

`new ps.reso.instaeclipse.mods.ui.StoryViewerSearchHook().install(dexKitBridge, hostClassLoader)` (284 ms)

Hooked (1):
- `X.0TVX#A07(java.util.List,boolean,boolean,boolean)void`

Log:
```
   [StoryViewerSearchHook.install:103] (IE|ViewerSearch) ✅ adapter X.0TVX.A07 rebuild=A14
```

## OK — CommentSearchHook

`new ps.reso.instaeclipse.mods.misc.CommentSearchHook().install(dexKitBridge, hostClassLoader)` (469 ms)

Hooked (2):
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#A07(X.0Cwz,X.0Cwy,X.0Cwy,X.0Cxi,X.0D3l,X.0DBq,X.0DBr,X.0Cvj,X.0Cwv,X.0D7l,com.instagram.comments.mvvm.viewmodel.CommentListViewModel,X.0Nxz,X.0DBs,X.0Gom,boolean,boolean,boolean)X.0ejk`
- `X.0M5A#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [CommentSearchHook.install:130] (IE|CommentSearch) comment json: serializer=true factory=true
   [CommentSearchHook.install:151] (IE|CommentSearch) ✅ vm com.instagram.comments.mvvm.viewmodel.CommentListViewModel builders=1 fragments=1
```

## OK — ChatExportHook

`new ps.reso.instaeclipse.mods.ghost.ChatExportHook().install(dexKitBridge, hostClassLoader)` (342 ms)

Hooked (1):
- `X.03GI#<init>(com.instagram.common.session.UserSession,X.02yB,java.util.List)`

Log:
```
   [ChatExportHook.install:124] (IE|ChatExport) ✅ store X.03GI messages=true serializer=true factory=X.02az
```

## FAIL — FollowListExportHook

`new ps.reso.instaeclipse.mods.misc.FollowListExportHook().install(dexKitBridge, hostClassLoader)` (198 ms)

Log:
```
!! [FollowListExportHook.install:83] (IE|FollowExport) ❌ FollowListFragment not found
```

## OK — LikeUndoHook

`new ps.reso.instaeclipse.mods.misc.LikeUndoHook().install(dexKitBridge)` (288 ms)

Hooked (1):
- `X.0Sm8#A03(android.content.Context,X.09rh,com.instagram.common.session.UserSession,X.0Whp,com.instagram.feed.media.Media,X.01Yz,com.instagram.search.common.analytics.SearchContext,java.lang.Double,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.Map,boolean)void`

Log:
```
   [LikeUndoHook.install:112] (InstaEclipse | LikeUndo): ✅ Hooked X.0Sm8.A03
```

## OK — ShareSheetCleanupHook

`new ps.reso.instaeclipse.mods.ui.ShareSheetCleanupHook().install(hostClassLoader)` (56 ms)

Hooked (2):
- `instagram.features.direct.fragment.sharesheet.DirectShareSheetFragment#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`

Log:
```
   [ShareSheetCleanupHook.install:99] (IE|ShareSheet) ✅ installed (2 hooks)
```

## NO-HOOK — ExternalBrowserHook

`new ps.reso.instaeclipse.mods.misc.ExternalBrowserHook().install()` (51 ms)

Log:
```
   [ExternalBrowserHook.install:44] (IE|ExtBrowser) ✅ hooked execStartActivity x0
```

## OK — DisableStoryFlippingHook

`new DisableStoryFlippingHook().handleStoryFlippingDisable(dexKitBridge)` (87 ms)

Hooked (1):
- `instagram.features.stories.fragment.ReelViewerFragment#A1h(com.instagram.model.reels.ReelItem)void`

Log:
```
   [DisableStoryFlippingHook.findAndHookMethod:84] (InstaEclipse | StoryFlipping): ✅ Hooked (dynamic check): instagram.features.stories.fragment.ReelViewerFragment.A1h
```

## PARTIAL — StoryMentionHook

`new StoryMentionHook().install(dexKitBridge, lpparam.classLoader)` (515 ms)

Hooked (9):
- `X.0NnR#A0x(X.00I2,X.0NnR,X.0BT1,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0y(X.0NnR)java.lang.CharSequence[]`
- `X.0NnR#A0z(X.0NnR,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0I(android.content.Context,X.0Zwh,com.instagram.model.reels.ReelItem,X.0NnR,X.067w)void`
- `X.0NnR#A0J(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Ody,X.0Odz,X.0Oeb,X.0Oec,X.0Oef,X.0NnR,X.068N,X.067u,X.068u,X.068F,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0L(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Oeb,X.0Oef,X.0NnR,X.068o,X.067u,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0M(android.content.DialogInterface$OnDismissListener,X.09rh,X.0Oeb,X.0Wtj,X.0Oee,X.0NnR,X.068u,X.068q,X.067w,X.068t,java.lang.CharSequence)void`
- `X.0NnR#A0h(java.util.List)void`
- `X.0NnR#A10(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.09rh,X.0vnm,X.0Oeb,X.0Wtj,X.0Oee,X.0kLk,X.068u,X.068q,X.067w,X.068t,X.068s)void`

Log:
```
!! [StoryMentionHook.resolveMentionPipeline:131] (IE|Mention) ❌ mentionsConverter not found
   [StoryMentionHook.installButtonHook:215] (IE|Mention) ✅ button hook installed on 3 builder(s)
   [StoryMentionHook.installClickHook:270] (IE|Mention) ✅ click hook installed
```

## OK — StoryLinksHook

`new ps.reso.instaeclipse.mods.misc.StoryLinksHook().install(dexKitBridge, lpparam.classLoader)` (356 ms)

Hooked (9):
- `X.0NnR#A0x(X.00I2,X.0NnR,X.0BT1,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0y(X.0NnR)java.lang.CharSequence[]`
- `X.0NnR#A0z(X.0NnR,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0I(android.content.Context,X.0Zwh,com.instagram.model.reels.ReelItem,X.0NnR,X.067w)void`
- `X.0NnR#A0J(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Ody,X.0Odz,X.0Oeb,X.0Oec,X.0Oef,X.0NnR,X.068N,X.067u,X.068u,X.068F,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0L(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Oeb,X.0Oef,X.0NnR,X.068o,X.067u,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0M(android.content.DialogInterface$OnDismissListener,X.09rh,X.0Oeb,X.0Wtj,X.0Oee,X.0NnR,X.068u,X.068q,X.067w,X.068t,java.lang.CharSequence)void`
- `X.0NnR#A0h(java.util.List)void`
- `X.0NnR#A10(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.09rh,X.0vnm,X.0Oeb,X.0Wtj,X.0Oee,X.0kLk,X.068u,X.068q,X.067w,X.068t,X.068s)void`

Log:
```
   [StoryLinksHook.installButtonHook:144] (IE|StoryLinks) ✅ button hook on 3 builder(s)
   [StoryLinksHook.installClickHook:190] (IE|StoryLinks) ✅ click hook installed
```

## OK — StoryInfoHook

`new ps.reso.instaeclipse.mods.misc.StoryInfoHook().install(dexKitBridge, lpparam.classLoader)` (1487 ms)

Hooked (9):
- `X.0NnR#A0x(X.00I2,X.0NnR,X.0BT1,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0y(X.0NnR)java.lang.CharSequence[]`
- `X.0NnR#A0z(X.0NnR,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0I(android.content.Context,X.0Zwh,com.instagram.model.reels.ReelItem,X.0NnR,X.067w)void`
- `X.0NnR#A0J(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Ody,X.0Odz,X.0Oeb,X.0Oec,X.0Oef,X.0NnR,X.068N,X.067u,X.068u,X.068F,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0L(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Oeb,X.0Oef,X.0NnR,X.068o,X.067u,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0M(android.content.DialogInterface$OnDismissListener,X.09rh,X.0Oeb,X.0Wtj,X.0Oee,X.0NnR,X.068u,X.068q,X.067w,X.068t,java.lang.CharSequence)void`
- `X.0NnR#A0h(java.util.List)void`
- `X.0NnR#A10(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.09rh,X.0vnm,X.0Oeb,X.0Wtj,X.0Oee,X.0kLk,X.068u,X.068q,X.067w,X.068t,X.068s)void`

Log:
```
   [StoryInfoHook.install:79] (IE|StoryInfo) getters: [taken_at, expiring_at, story_edited_at, is_story_edited, music_metadata, story_music_stickers, story_music_lyric_stickers, clips_metadata, story_locations, story_hashtags, reshared_story_media_author, story_feed_media, story_app_attribution, accessibility_caption, original_width, original_height, has_audio, story_countdowns, story_questions, story_mention_request_info, story_quizs, story_polls, story_sliders, story_prompts, sponsor_tags, is_paid_partnership, coauthor_producers]
   [StoryInfoHook.installButtonHook:175] (IE|StoryInfo) ✅ button hook on 3 builder(s)
   [StoryInfoHook.installClickHook:228] (IE|StoryInfo) ✅ click hook installed
```

## OK — CommentCopyHook

`new CommentCopyHook().install(dexKitBridge, lpparam.classLoader)` (235 ms)

Hooked (4):
- `X.0M5A#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#GP6(java.lang.String,java.lang.String,float,boolean)void`

Log:
```
   [CommentCopyHook.findAndHookNew:198] (InstaEclipse | CopyComment): ✅ Hooked com.instagram.comments.mvvm.viewmodel.CommentListViewModel.GP6 (resolver=X.0DCn.A01)
```

## PARTIAL — CaptionCopyContextMenuHook

`new CaptionCopyContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (295 ms)

Hooked (6):
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `X.0Ckv#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Ckv#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Mgn#A09(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0cnG#A01(boolean)java.util.List`

Log:
```
!! [CaptionCopyContextMenuHook.loadMediaOptionEnum:174] (IE|Caption) ❌ loadMediaOptionEnum: java.lang.reflect.InvocationTargetException
   [CaptionCopyContextMenuHook.resolveCaptionGetter:221] (IE|Caption) ✅ captionGetter=com.instagram.feed.media.Media.A32
!! [CaptionCopyContextMenuHook.installAddButtonHook:425] (IE|Caption) ❌ Cannot install addButton hook — prerequisites missing
```

## OK — DisableDoubleTapLikeHook

`new DisableDoubleTapLikeHook().install(dexKitBridge, lpparam.classLoader)` (779 ms)

Hooked (3):
- `X.0Dao#A01(android.app.Activity,X.03yW,com.instagram.common.session.UserSession,com.instagram.feed.media.Media,X.01Yz,X.0Kel,com.instagram.search.common.analytics.SearchContext,X.0WgF,java.lang.Double,java.lang.Integer,java.lang.Long,java.lang.String,java.lang.String,int,int,int,boolean)void`
- `X.0Sm8#A02(android.app.Activity,android.content.Context,X.03yW,com.instagram.common.session.UserSession,X.0Whp,com.instagram.feed.media.Media,X.01Yz,X.092z,X.0Kel,X.0ezo,com.instagram.search.common.analytics.SearchContext,X.0WgF,java.lang.Double,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,int,int,int,boolean)void`
- `X.0JdR#invoke(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [DisableDoubleTapLikeHook.findAndHook:139] (InstaEclipse | DoubleTapLike): Feed hooked on X.0Dao.A01
   [DisableDoubleTapLikeHook.findAndHook:139] (InstaEclipse | DoubleTapLike): Feed hooked on X.0Sm8.A02
   [DisableDoubleTapLikeHook.findAndHook:161] (InstaEclipse | DoubleTapLike): Reels like action hooked on X.0JdR.invoke
```

## OK — DoubleTapExtrasHook

`new ps.reso.instaeclipse.mods.misc.DoubleTapExtrasHook().install(dexKitBridge, lpparam.classLoader)` (291 ms)

Hooked (6):
- `X.0EcE#onDoubleTap(android.view.MotionEvent)boolean`
- `X.0Ecb#onDoubleTap(android.view.MotionEvent)boolean`
- `X.0ZdC#FW1()void`
- `X.0giW#Fwr(X.02xK,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.0gjX#Fwr(X.02xK,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.String,java.lang.String,java.util.List,boolean)void`
- `X.08Rv#A00(X.0GhL,X.02xK,X.02nf,java.lang.String,java.lang.String,java.util.List,boolean)void`

Log:
```
   [DoubleTapExtrasHook.install:101] (InstaEclipse | DoubleTapLike): ✅ comments 2, DM senders 3, DM double-tap 1
```

## OK — FeedPhotoZoomHook

`new FeedPhotoZoomHook().install(lpparam.classLoader)` (53 ms)

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

`new IgThemeHook().install(hostClassLoader)` (63 ms)

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

`new ForceReelQualityHook().install(dexKitBridge, lpparam.classLoader)` (139 ms)

Hooked (1):
- `com.instagram.feed.media.Media#AAM()java.util.List`

Log:
```
   [ForceReelQualityHook.install:129] (InstaEclipse | ForceReelQuality): ✅ Hooked com.instagram.feed.media.Media#AAM (height=CHg)
```

## FAIL — MaxUploadQualityHook

`new ps.reso.instaeclipse.mods.media.MaxUploadQualityHook().install(dexKitBridge, lpparam.classLoader)` (161 ms)

Log:
```
!! [MaxUploadQualityHook.install:60] (IE|MaxUploadQuality) ❌ highest-quality-upload getter not found
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
- `X.04jN#A00(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DisableVideoAutoPlayHook.hookManualPlayOverlayLifecycle:121] (InstaEclipse | AutoPlayDisable): Hooked manual play overlay lifecycle.
   [DisableVideoAutoPlayHook.hookMethod:94] (InstaEclipse | AutoPlayDisable): ✅ Hooked (dynamic check): X.04jN.A00
```

## OK — BuildExpiredPopupHook

`new BuildExpiredPopupHook().install(dexKitBridge, lpparam.classLoader)` (279 ms)

Hooked (2):
- `X.02zV#A02(androidx.fragment.app.FragmentActivity,X.0mlb)void`
- `X.02zV#A04()boolean`

Log:
```
   [BuildExpiredPopupHook.install:86] (IE|BuildExpired) ✅ hooked show-popup → X.02zV.A02
   [BuildExpiredPopupHook.install:109] (IE|BuildExpired) ✅ hooked snooze-check → X.02zV.A04
```

## NO-HOOK — FeedVideoDownloadHook

`new FeedVideoDownloadHook().install(lpparam.classLoader)` (59 ms)

Log:
```
   [FeedVideoDownloadHook.install:168] (IE|DL) image url method=A0Q (ExtendedImageUrl)
   [FeedVideoDownloadHook.install:186] (IE|DL) media model: mutable=false liveTree=false listCandidates=0
```

## FAIL — FeedVideoDownloadHook

`FeedVideoDownloadHook.installVideoUrlCaptureHook(dexKitBridge, lpparam.classLoader)` (2066 ms)

Log:
```
   [SelectedMediaAdapter.install:61] (IE|MediaAdapter) resolved properties=13 manifest=true direct=true story=true
   [FeedVideoDownloadHook.discoverDynamicMediaModel:1251] (IE|DL|DexKit) dynamic media dict=X.09AK
   [FeedVideoDownloadHook.resolveVideoVersionsGetters:1307] (IE|DL|DexKit) video_versions getters=1
   [FeedVideoDownloadHook.resolveCarouselGetter:1352] (IE|DL|DexKit) carousel getter=com.instagram.feed.media.Media.A8U
   [FeedVideoDownloadHook.resolveDashManifestGetter:1421] (IE|DL|DexKit) dash manifest getter=com.instagram.feed.media.Media.A89
   [FeedVideoDownloadHook.resolveHasAudioGetter:1379] (IE|DL|DexKit) has_audio getter=A3l
   [FeedVideoDownloadHook.resolveMediaGetter:1614] (IE|DL|DexKit) music_metadata getter=A29
   [FeedVideoDownloadHook.resolveMediaGetter:1614] (IE|DL|DexKit) clips_metadata getter=A2y
!! [FeedVideoDownloadHook.resolveIsVideoMethod:1752] (IE|DL|DexKit) isVideo=not found
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1177] (IE|DL|DexKit) VideoVersionIntf implementors found: 0
   [FeedVideoDownloadHook.resolveUsernameGetter:1796] (IE|DL|Username) userClass=com.instagram.user.model.User
   [FeedVideoDownloadHook.resolveUsernameGetter:1837] (IE|DL|Username) userUsernameGetter=A7i (excluded 0 full_name getter(s))
!! [FeedVideoDownloadHook.resolveDictUserGetter:1959] (IE|DL|Username) ❌ Failed to resolve dictUserGetter in hierarchy
   [FeedVideoDownloadHook.resolveMediaAuthorGetter:1882] (IE|DL|Username) ✅ mediaAuthorGetter (Media."user"): A3M
```

## PARTIAL — PostDownloadContextMenuHook

`new PostDownloadContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (363 ms)

Hooked (4):
- `X.0Ckv#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Ckv#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Mgn#A09(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0cnG#A01(boolean)java.util.List`

Log:
```
!! [PostDownloadContextMenuHook.loadMediaOptionEnum:134] (IE|Post) ❌ loadMediaOptionEnum: java.lang.reflect.InvocationTargetException
!! [PostDownloadContextMenuHook.findCreatorClassAndAddButtonMethod:268] (IE|Post) ❌ findCreatorClassAndAddButtonMethod: java.lang.reflect.InvocationTargetException
!! [PostDownloadContextMenuHook.installAddButtonHook:277] (IE|Post) ❌ Cannot install addButton hook — prerequisites missing
   [PostDownloadContextMenuHook.resolveReelClickDispatcher:381] (IE|Post) selected Reel dispatcher bound=false
   [PostDownloadContextMenuHook.installAllowlistPatchHook:535] (IE|Post) ✅ Allowlist patch hooked: X.0cnG.A01
```

## OK — InstantSaveHook

`new ps.reso.instaeclipse.mods.media.InstantSaveHook().install(lpparam.classLoader)` (63 ms)

Hooked (2):
- `com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1#<init>(androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,X.0X94,com.instagram.common.session.UserSession,com.instagram.compose.ui.emitter.AnimatedParticleEffectState,X.03Et,java.util.List,java.util.List,X.0UAG,int,boolean)`
- `com.instagram.quicksnap.viewer.compose.PointerTouchKt$handleTapAndLongPressWithRelease$2$1$1$1#<init>(X.0UAG,kotlin.jvm.functions.Function0,kotlin.jvm.functions.Function1,kotlin.jvm.functions.Function1,float,long,boolean)`

Log:
```
   [InstantSaveHook.cacheCurrentInstant:75] (IE|InstantSave) ✅ viewer cache hooked: com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1
   [InstantSaveHook.wrapLongPress:180] (IE|InstantSave) ✅ long-press hooked
```

## PARTIAL — InstantUploadHook

`new ps.reso.instaeclipse.mods.media.InstantUploadHook().install(lpparam.classLoader)` (57 ms)

Hooked (9):
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A08()void`
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
!! [InstantUploadHook.hookSwap:84] (IE|InstantUpload) photo handoff unresolved or ambiguous
   [InstantUploadHook.hookCameraOpen:219] (IE|InstantUpload) ✅ camera-open hooked (8 VM methods)
   [InstantUploadHook.hookPickerResult:666] (IE|InstantUpload) ✅ picker-result hooked
```

## PARTIAL — GhostEphemeralKeepHook

`new GhostEphemeralKeepHook().install(dexKitBridge, lpparam.classLoader)` (397 ms)

Hooked (5):
- `X.03GI#A0b(boolean)boolean`
- `X.06ec#HCv(X.02sx,X.0XoP,X.0ArI)void`
- `X.0005#A00(int)java.lang.String`
- `X.02wQ#unsafeParseFromJson(X.088A)java.lang.Object`
- `X.02wQ#parseFromJsonParser(X.088A)java.lang.Object`

Log:
```
!! [GhostEphemeralKeepHook.hookVanishLocalDelete:88] (IE|Ephemeral) ⚠️ vanish-mode purge ambiguous (2 ()Z methods)
   [GhostEphemeralKeepHook.hookVanishLocalDelete:94] (IE|Ephemeral) ✅ local purge hook → X.03GI.A0b
   [GhostEphemeralKeepHook.hookServerPing:169] (IE|Ephemeral) ✅ server-ping hook → X.06ec.HCv
   [GhostEphemeralKeepHook.hookExpiryParser:235] (IE|Ephemeral) ✅ expiry-parser hook → X.0005.A00
   [GhostEphemeralKeepHook.hookExpiryParser:235] (IE|Ephemeral) ✅ expiry-parser hook → X.02wQ.unsafeParseFromJson
   [GhostEphemeralKeepHook.hookExpiryParser:235] (IE|Ephemeral) ✅ expiry-parser hook → X.02wQ.parseFromJsonParser
```

## OK — GhostPermanentViewHook

`new GhostPermanentViewHook().install(dexKitBridge, lpparam.classLoader)` (169 ms)

Hooked (1):
- `X.0000#A00(int)java.lang.String`

Log:
```
   [GhostPermanentViewHook.install:96] (IE|ViewOnceMedia) ✅ hooking X.0000.A00
   [GhostPermanentViewHook.install:103] (IE|ViewOnceMedia) ✅ hooked
```

## OK — ViewOnceBadgeHook

`new ViewOnceBadgeHook().install(dexKitBridge, lpparam.classLoader)` (77 ms)

Hooked (1):
- `X.08XG#<init>(X.02HI,X.02GX,X.08XF,com.instagram.direct.thread.analytics.DirectThreadAnalyticsParams,com.instagram.direct.wellbeing.harmfulcontent.ondevicenudity.view.data.PrivacyMediaOverlayViewModel,X.0AHT,java.lang.String,java.lang.String,boolean,boolean,boolean,boolean)`

Log:
```
   [ViewOnceBadgeHook.install:75] (IE|VOBadge) ✅ hooked X.08XG.<init>
```

## OK — LockedCollectionsHook

`new ps.reso.instaeclipse.mods.ui.LockedCollectionsHook().install(hostClassLoader)` (52 ms)

Hooked (1):
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [LockedCollectionsHook.install:77] (IE|LockedColl) ✅ installed
```

## FAIL — SavedOnProfileHook

`new ps.reso.instaeclipse.mods.ui.SavedOnProfileHook().install(dexKitBridge, hostClassLoader)` (51 ms)

Log:
```
!! [SavedOnProfileHook.install:97] (IE|SavedProfile) ❌ factory=null navigator=null navMake=null
```

## OK — HideHeaderButtonsHook

`new ps.reso.instaeclipse.mods.ui.HideHeaderButtonsHook().install(hostClassLoader)` (51 ms)

Hooked (1):
- `android.app.Activity#onResume()void`

Log:
```
   [HideHeaderButtonsHook.install:46] (IE|HideButtons) ✅ installed
```

## OK — ShareLinkDownloadHook

`new ps.reso.instaeclipse.mods.media.ShareLinkDownloadHook().install(hostClassLoader)` (52 ms)

Hooked (3):
- `com.instagram.common.session.UserSession#<init>(X.08Jf,java.lang.String,X.02as,boolean,boolean)`
- `android.app.Activity#onCreate(android.os.Bundle)void`
- `android.app.Activity#onNewIntent(android.content.Intent)void`

Log:
```
   [ShareLinkDownloadHook.install:71] (IE|ShareDL) ✅ installed
```

## OK — StoryDownloadHook

`new StoryDownloadHook().install(dexKitBridge, lpparam.classLoader)` (978 ms)

Hooked (14):
- `X.0NnR#A0x(X.00I2,X.0NnR,X.0BT1,boolean)java.lang.CharSequence[]`
- `X.0NnR#A0y(X.0NnR)java.lang.CharSequence[]`
- `X.0NnR#A0z(X.0NnR,boolean)java.lang.CharSequence[]`
- `X.0NnR#A09(X.0NnR)java.util.ArrayList`
- `X.0NnR#A0A(X.0NnR,boolean)java.util.ArrayList`
- `X.0NnR#A0I(android.content.Context,X.0Zwh,com.instagram.model.reels.ReelItem,X.0NnR,X.067w)void`
- `X.0NnR#A0J(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Ody,X.0Odz,X.0Oeb,X.0Oec,X.0Oef,X.0NnR,X.068N,X.067u,X.068u,X.068F,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0L(android.content.DialogInterface$OnDismissListener,X.09rh,X.0WiA,X.0Oeb,X.0Oef,X.0NnR,X.068o,X.067u,X.067w,java.lang.CharSequence)void`
- `X.0NnR#A0M(android.content.DialogInterface$OnDismissListener,X.09rh,X.0Oeb,X.0Wtj,X.0Oee,X.0NnR,X.068u,X.068q,X.067w,X.068t,java.lang.CharSequence)void`
- `X.0NnR#A0h(java.util.List)void`
- `X.0NnR#A10(android.content.Context,android.content.DialogInterface$OnDismissListener,android.view.View,X.09rh,X.0vnm,X.0Oeb,X.0Wtj,X.0Oee,X.0kLk,X.068u,X.068q,X.067w,X.068t,X.068s)void`
- `X.0O1f#A0H(android.view.View,X.0YmM,com.instagram.model.reels.ReelItem,java.lang.Integer)void`
- `X.0PHJ#A00()java.util.List`
- `instagram.features.stories.fragment.ReelViewerFragment#FS2(com.instagram.model.reels.ReelItem,X.0Ooq)void`

Log:
```
   [StoryDownloadHook.install:184] (IE|Highlight) reel class X.04ot
   [StoryDownloadHook.installButtonInjectorHook:591] (IE|Story) button injector hooked 3 builder(s)
   [StoryDownloadHook.installHighlightButtonInjectorHook:656] (IE|Story) Highlight injector hooked 2 builder(s)
   [StoryDownloadHook.installClickHandlerHook:790] (IE|Story) click handler hooked 6 dispatcher(s)
   [StoryDownloadHook.installHighlightTrayHook:282] (IE|HighlightTray) ✅ X.0O1f legacy=true popup=false loader=false
   [StoryDownloadHook.installStoryCaptureHook:440] (IE|StoryCache) capture hook: 1 method(s)
```

## OK — ReelDownloadHook

`new ReelDownloadHook().install(dexKitBridge, lpparam.classLoader)` (466 ms)

Hooked (5):
- `X.0Ckr#A06(X.0BX4,X.075A,X.0Ckr)java.util.ArrayList`
- `X.071o#A07(X.0ELB,com.instagram.common.session.UserSession)boolean`
- `X.0Ckr#A0B(android.graphics.PointF,android.view.View,android.view.View,X.075A,X.0Ddj,X.0NHx,X.0Ckr)void`
- `X.0Ckr#A0J(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PHJ)void`
- `X.0Ckr#A0K(com.instagram.feed.media.mediaoption.MediaOption$Option,X.0PHJ)void`

Log:
```
   [ReelDownloadHook.installGateHook:502] (IE|Reel) gate config 36313978552585585 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installGateHook:502] (IE|Reel) gate config 36313978552847731 absent (pre-457 gate; 457 uses the sheet row injection)
   [ReelDownloadHook.installReduceOptionsListPatch:240] (IE|Reel) ✅ Options-list patch hooked: X.0Ckr.A06
   [ReelDownloadHook.install:81] (IE|Reel) string matches=4 candidateClasses=3
   [ReelDownloadHook.installEligibilityGate457:419] (IE|Reel) ✅ 457 eligibility gate: X.071o.A07
   [ReelDownloadHook.installSheetRowInjection:363] (IE|Reel) ✅ sheet row injection: X.0Ckr builders=1 rowAdders=2
```

## OK — ProfilePicDownloadHook

`ProfilePicDownloadHook.install(dexKitBridge, hostClassLoader)` (69 ms)

Hooked (2):
- `androidx.fragment.app.Fragment#onResume()void`
- `android.view.View#onAttachedToWindow()void`

Log:
```
   [ProfilePicDownloadHook.resolveHdGetter:120] (IE|ProfileDL) ✅ HD getter User.A1D
```

## OK — interceptor

`interceptor.handleInterceptor(lpparam)` (66 ms)

Hooked (2):
- `com.instagram.api.tigon.TigonServiceLayer#startRequest(X.03wd,X.03wu,X.04kA)X.0A0F`
- `X.01xv#startRequest(X.03wd,X.03wu,X.04kA)X.0A0F`

Log:
```
   [IGNetworkInterceptor.hookChainEntry:76] (InstaEclipse | Interceptor): chain-entry hooks: 1
```

## OK — TerminatedExecutorGuard

`new ps.reso.instaeclipse.mods.core.TerminatedExecutorGuard().install(lpparam.classLoader)` (51 ms)

Hooked (1):
- `java.util.concurrent.ThreadPoolExecutor$AbortPolicy#rejectedExecution(java.lang.Runnable,java.util.concurrent.ThreadPoolExecutor)void`

Log:
```
   [TerminatedExecutorGuard.install:53] (IE|ExecGuard) ✅ installed
```

