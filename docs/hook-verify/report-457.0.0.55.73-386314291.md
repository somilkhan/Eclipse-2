# hook-verify report

APK: `ig457.apk` — Instagram 457.0.0.55.73 (386314291)

Results hold for this exact build only; a device running another build of the same major version can resolve different classes.

Status legend: **OK** = hooks installed and no failure lines; **PARTIAL** = some hooks installed but at least one sub-lookup logged a failure; **FAIL** = failure logged and nothing hooked; **NO-HOOK** = nothing hooked and nothing logged (usually a hook that installs lazily at runtime, or one that only hooks framework classes once an Activity exists); **ERROR** = install threw. **(harness)** = every failure line comes from the offline harness (stubbed Instagram code or mockable android.jar), not from a lookup; confirm on a device.

```
OK       instagramUI                      hooks=5      502 ms
OK       DevOptionsUnlockHook             hooks=1     1023 ms
OK       GhostDMSeenHook                  hooks=1      166 ms
OK       GhostDMMarkAsReadHook            hooks=1       52 ms
OK       GhostChannelMarkAsReadHook       hooks=1       52 ms
OK       GhostTypingIndicatorHook         hooks=1      170 ms
OK       GhostScreenshotDetectionHook     hooks=1      153 ms
OK       ScreenshotPermissionHook         hooks=2       54 ms
OK       GhostViewOnceHook                hooks=1      175 ms
OK       GhostStorySeenHook               hooks=6      168 ms
OK       KeepUnsentMessagesHook           hooks=7     1065 ms
OK       UnsentThreadButtonHook           hooks=5       57 ms
OK       HideChatsHook                    hooks=3      282 ms
OK       CustomFontHook                   hooks=9       75 ms
OK       metaAi                           hooks=2       52 ms
OK       metaAi                           hooks=8     1721 ms
OK       DisableRepostHook                hooks=2      432 ms
OK       LockDirectMessagesHook           hooks=1       52 ms
OK       HideSuggestedFeedItemsHook       hooks=2      425 ms
OK       AdBlocker                        hooks=1      169 ms
OK       TrackingLinkDisable              hooks=5       55 ms
OK       ExactTimestampHook               hooks=1      146 ms
OK       ExpandCaptionsHook               hooks=2      184 ms
OK       HideCommunityNotesHook           hooks=2       65 ms
OK       ReelsControlsHook                hooks=3      917 ms
OK       AccountLimitHook                 hooks=1      723 ms
OK       AirplaneModeHook                 hooks=3       55 ms
OK       HighResolutionImageHook          hooks=1      239 ms
OK       OnboardingPromptHook             hooks=1      166 ms
NO-HOOK  NotificationRegistrationHook     hooks=0       51 ms
OK       StartupTabHook                   hooks=4       52 ms
OK       SwipeToCameraHook                hooks=1       53 ms
OK       FollowingOnlyFeedHook            hooks=5      165 ms
OK       ProfileFollowsYouHook            hooks=2      134 ms
OK       PlaybackSpeedHook                hooks=23    1078 ms
OK       HideNotesTrayHook                hooks=1      188 ms
PARTIAL  VoiceMessageDownloadHook         hooks=6      693 ms  (harness)
OK       EditHistoryHook                  hooks=2      251 ms
NO-HOOK  JumpToFirstMessageHook           hooks=0       51 ms
NO-HOOK  ReadReceiptChatHook              hooks=0       51 ms
OK       StoryViewerSearchHook            hooks=1      312 ms
OK       CommentSearchHook                hooks=2      485 ms
OK       ChatExportHook                   hooks=1      354 ms
OK       FollowListExportHook             hooks=1      348 ms
OK       LikeUndoHook                     hooks=1      296 ms
OK       ShareSheetCleanupHook            hooks=2       58 ms
NO-HOOK  ExternalBrowserHook              hooks=0       51 ms
OK       DisableStoryFlippingHook         hooks=1      154 ms
OK       StoryMentionHook                 hooks=9      639 ms
OK       StoryLinksHook                   hooks=9      361 ms
OK       StoryInfoHook                    hooks=9     1548 ms
OK       CommentCopyHook                  hooks=4      227 ms
OK       CaptionCopyContextMenuHook       hooks=11    1233 ms
OK       DisableDoubleTapLikeHook         hooks=3      783 ms
OK       DoubleTapExtrasHook              hooks=6      329 ms
OK       FeedPhotoZoomHook                hooks=1       54 ms
FAIL     LocationSpoofHook                hooks=0       52 ms  (harness)
OK       IgThemeHook                      hooks=21      62 ms
OK       ForceReelQualityHook             hooks=1      111 ms
OK       MaxUploadQualityHook             hooks=1      440 ms
OK       DisableVideoAutoPlayHook         hooks=9      173 ms
OK       BuildExpiredPopupHook            hooks=2      282 ms
NO-HOOK  FeedVideoDownloadHook            hooks=0       59 ms
OK       FeedVideoDownloadHook            hooks=2     2244 ms
OK       PostDownloadContextMenuHook      hooks=5      361 ms
OK       InstantSaveHook                  hooks=2       55 ms
OK       InstantUploadHook                hooks=10      59 ms
OK       GhostEphemeralKeepHook           hooks=6      413 ms
OK       GhostPermanentViewHook           hooks=1      175 ms
OK       ViewOnceBadgeHook                hooks=1       79 ms
OK       LockedCollectionsHook            hooks=1       51 ms
OK       SavedOnProfileHook               hooks=1       57 ms
OK       HideHeaderButtonsHook            hooks=1       51 ms
OK       ShareLinkDownloadHook            hooks=3       52 ms
OK       StoryDownloadHook                hooks=14    1049 ms
OK       ReelDownloadHook                 hooks=5      485 ms
OK       ProfilePicDownloadHook           hooks=2       69 ms
OK       interceptor                      hooks=2       75 ms
OK       TerminatedExecutorGuard          hooks=1       51 ms
TOTAL: 79 steps  OK=72 PARTIAL=1 FAIL=1 NO-HOOK=5 ERROR=0
```

## OK — instagramUI

`instagramUI.mainActivity(hostClassLoader)` (502 ms)

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

## OK — DevOptionsUnlockHook

`new DevOptionsUnlockHook().handleDevOptions(dexKitBridge)` (1023 ms)

Hooked (1):
- `X.02ua#A01(com.instagram.common.session.UserSession)boolean`

Log:
```
   [DevOptionsUnlockHook.findAndHookDynamicMethod:73] (InstaEclipse | DevOptionsEnable): 🔍 Discovery Tier 1 (String)...
   [DevOptionsUnlockHook.findAndHookDynamicMethod:105] (InstaEclipse | DevOptionsEnable): ℹ️ Tier 1 (String) found no gate on this build, trying Tier 2 (Structural)...
   [DevOptionsUnlockHook.hookExactMethod:224] (InstaEclipse | DevOptionsEnable): ✅ Hooked: X.02ua.A01
   [DevOptionsUnlockHook.findAndHookDynamicMethod:112] (InstaEclipse | DevOptionsEnable): 🎯 Found via structural match: X.02ua.A01
```

## OK — GhostDMSeenHook

`new GhostDMSeenHook().handleSeenBlock(dexKitBridge)` (166 ms)

Hooked (1):
- `X.0Atf#A0A(com.instagram.common.session.UserSession,X.02wI,java.lang.String,java.lang.String,java.lang.String)void`

Log:
```
   [GhostDMSeenHook.handleSeenBlock:86] (InstaEclipse | GhostModeSeen): ✅ Hooked: X.0Atf.A0A
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

`new GhostTypingIndicatorHook().handleTypingBlock(dexKitBridge)` (170 ms)

Hooked (1):
- `X.06Zw#A0S(boolean)void`

Log:
```
   [GhostTypingIndicatorHook.handleTypingBlock:86] (InstaEclipse | TypingBlock): ✅ Hooked (dynamic check): X.06Zw.A0S
```

## OK — GhostScreenshotDetectionHook

`new GhostScreenshotDetectionHook().handleScreenshotBlock(dexKitBridge)` (153 ms)

Hooked (1):
- `X.07WF#GKZ(long)void`

Log:
```
   [GhostScreenshotDetectionHook.handleScreenshotBlock:74] (InstaEclipse | ScreenshotBlock): ✅ Hooked (dynamic check): X.07WF.GKZ
```

## OK — ScreenshotPermissionHook

`new ScreenshotPermissionHook().install(lpparam.classLoader)` (54 ms)

Hooked (2):
- `android.view.Window#setFlags(int,int)void`
- `android.view.Window#addFlags(int)void`

Log:
```
   [ScreenshotPermissionHook.install:47] (InstaEclipse | ScreenshotPermission): ✅ Hooked Window.setFlags + addFlags
```

## OK — GhostViewOnceHook

`new GhostViewOnceHook().handleViewOnceBlock(dexKitBridge)` (175 ms)

Hooked (1):
- `X.05tf#HCv(X.02sx,X.0XoP,X.0ArI)void`

Log:
```
   [GhostViewOnceHook.tryHookViewOnce:77] (InstaEclipse | ViewOnce): ✅ Hooked (endpoint+raven_media): X.05tf.HCv
```

## OK — GhostStorySeenHook

`new GhostStorySeenHook().handleStorySeenBlock(dexKitBridge)` (168 ms)

Hooked (6):
- `X.019u#A0E(java.lang.String,java.lang.Object)void`
- `X.019u#A0F(java.util.Map)void`
- `X.019u#A0R(java.lang.String,java.lang.Object)void`
- `X.02pM#A0K(java.lang.Object)void`
- `X.02pM#A0S(X.02pN)void`
- `X.02pM#A0T(X.02pN)void`

Log:
```
   [GhostStorySeenHook.tryModernBatchBlock:103] (InstaEclipse | StoryBlock): ✅ Hooked 6 sender(s) on store X.02pM (item builder: X.02pN.A06)
```

## OK — KeepUnsentMessagesHook

`new KeepUnsentMessagesHook().install(dexKitBridge, lpparam.classLoader)` (1065 ms)

Hooked (7):
- `androidx.fragment.app.Fragment#performResume()void`
- `androidx.fragment.app.Fragment#performPause()void`
- `androidx.fragment.app.Fragment#performDestroyView()void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#H21(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String,boolean)void`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#H20(com.instagram.model.direct.DirectThreadKey,java.lang.String,java.lang.String)void`
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
   [KeepUnsentMessagesHook.install:93] (IE|KeepUnsent) sender binding=false; optional in-chat binding=false
   [KeepUnsentMessagesHook.resolveEntryClass:395] (IE|KeepUnsent) ✅ thread entry → X.03GI
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.H21 (4-arg)
   [KeepUnsentMessagesHook.hookRemovePrimitive:296] (IE|KeepUnsent) ✅ remove hook → com.instagram.direct.store.impl.DirectThreadStoreImpl.H20 (3-arg)
   [CurrentThreadTracker.install:95] (IE|ThreadTracker) ✅ Fragment onResume/onPause hooked
   [KeepUnsentMessagesHook.hookCurrentThread:135] (IE|KeepUnsent) current-thread tracker hooked 0 method(s)
```

## OK — UnsentThreadButtonHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.UnsentThreadButtonHook().install(lpparam.classLoader)` (57 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.ghost.HideChatsHook().install(dexKitBridge, lpparam.classLoader)` (282 ms)

Hooked (3):
- `X.00BN#<init>(android.content.Context,X.04om,com.instagram.common.session.UserSession,com.instagram.direct.store.impl.DirectThreadStoreImpl,com.instagram.direct.store.impl.sqlite.DirectSQLiteDiskIO,X.00B6,java.lang.Object)`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0d(X.00EF,X.0HjN,java.util.Comparator,java.util.List,boolean)java.util.ArrayList`
- `com.instagram.direct.store.impl.DirectThreadStoreImpl#A0e(java.util.List)java.util.ArrayList`

Log:
```
   [HiddenInboxFilter.installRefresh:121] (IE|HideChats) local inbox refresh method resolved
   [HiddenInboxFilter.install:93] (IE|HideChats) inbox methods resolved=2; execution requires a real inbox
```

## OK — CustomFontHook

`new io.github.somilkhan.instaeclipse2.mods.ui.CustomFontHook().install(lpparam.classLoader)` (75 ms)

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

`metaAi.install(lpparam.classLoader)` (52 ms)

Hooked (2):
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup,boolean)android.view.View`
- `android.view.LayoutInflater#inflate(int,android.view.ViewGroup)android.view.View`

Log:
```
   [RemoveMetaAIHook.install:126] (IE|RemoveMetaAI) ✅ installed
```

## OK — metaAi

`metaAi.installReels(dexKitBridge, lpparam.classLoader)` (1721 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.ui.DisableRepostHook().install(dexKitBridge, lpparam.classLoader)` (432 ms)

Hooked (2):
- `X.0Mnv#EF5(android.view.View,X.010B,X.0Gv1,java.lang.String,java.lang.String,java.lang.String,kotlin.jvm.functions.Function0,int)void`
- `X.0Agr#A0R(X.0Ktu,kotlin.jvm.functions.Function1)void`

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

`new HideSuggestedFeedItemsHook().install(dexKitBridge, hostClassLoader)` (425 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.misc.ExactTimestampHook().install(dexKitBridge)` (146 ms)

Hooked (1):
- `X.03fs#A08(android.content.res.Resources,X.04TA,X.03fs,java.lang.Integer,double,double,boolean,boolean,boolean,boolean)java.lang.String`

Log:
```
   [ExactTimestampHook.install:84] (InstaEclipse | ExactTime): ✅ Hooked X.03fs.A08
```

## OK — ExpandCaptionsHook

`new io.github.somilkhan.instaeclipse2.mods.feed.ExpandCaptionsHook().install(dexKitBridge)` (184 ms)

Hooked (2):
- `X.02oM#A03(android.content.Context,X.05i1,com.instagram.search.common.analytics.SearchContext,X.02oM,java.lang.String,java.lang.String,boolean)android.text.SpannableStringBuilder`
- `X.05k3#A00(X.02oW,java.lang.CharSequence,java.lang.CharSequence,java.lang.CharSequence,int,boolean)java.lang.CharSequence`

Log:
```
   [ExpandCaptionsHook.install:67] (InstaEclipse | ExpandCaptions): ✅ Hooked X.02oM.A03 → X.05k3.A00
```

## OK — HideCommunityNotesHook

`new io.github.somilkhan.instaeclipse2.mods.feed.HideCommunityNotesHook().install(dexKitBridge, hostClassLoader)` (65 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoMediaOverlayPayloadSchema#Cqf()X.02ES`
- `com.instagram.api.schemas.MediaOverlayPayloadSchema#Cqf()X.02ES`

Log:
```
   [HideCommunityNotesHook.install:71] (InstaEclipse | CommunityNotes): ✅ notes getter Cqf hooked on 2 class(es)
```

## OK — ReelsControlsHook

`new io.github.somilkhan.instaeclipse2.mods.extras.ReelsControlsHook().install(dexKitBridge, hostClassLoader)` (917 ms)

Hooked (3):
- `X.0AMH#A09(com.instagram.common.session.UserSession,boolean)boolean`
- `X.0CBZ#E59(int)android.view.View`
- `instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout#onInterceptTouchEvent(android.view.MotionEvent)boolean`

Log:
```
   [ReelsControlsHook.installAutoScroll:66] (InstaEclipse | Reels): ✅ auto-scroll gate X.0AMH.A09
   [ReelsControlsHook.installScrollLock:97] (InstaEclipse | Reels): ✅ pager X.0CBZ.A0H
```

## OK — AccountLimitHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AccountLimitHook().install(dexKitBridge, hostClassLoader)` (723 ms)

Hooked (1):
- `X.0BJH#A01(X.08Ji,int)boolean`

Log:
```
   [AccountLimitHook.install:38] (InstaEclipse | UnlimitedAccounts): ✅ hooked X.0BJH.A01
```

## OK — AirplaneModeHook

`new io.github.somilkhan.instaeclipse2.mods.extras.AirplaneModeHook().install(hostClassLoader)` (55 ms)

Hooked (3):
- `com.instagram.realtimeclient.RealtimeClientManager#<init>(android.content.Context,com.instagram.common.session.UserSession,com.instagram.realtimeclient.clientconfig.RealtimeClientConfig,com.instagram.realtimeclient.MainRealtimeEventHandler,X.0GeL)`
- `com.instagram.realtimeclient.RealtimeClientManager#startMqttClient()void`
- `com.instagram.realtimeclient.MainRealtimeEventHandler#onRealtimeEvent(java.lang.String,com.instagram.realtimeclient.RealtimeEvent)void`

Log:
```
   [AirplaneModeHook.install:84] (InstaEclipse | Airplane): ✅ hooked
```

## OK — HighResolutionImageHook

`new io.github.somilkhan.instaeclipse2.mods.extras.HighResolutionImageHook().install(dexKitBridge, hostClassLoader)` (239 ms)

Hooked (1):
- `X.03kB#A02(java.lang.Integer,java.util.List,int)com.instagram.common.typedurl.ImageUrl`

Log:
```
   [HighResolutionImageHook.install:88] (InstaEclipse | HighResImages): ✅ hooked 1 picker(s)
```

## OK — OnboardingPromptHook

`new io.github.somilkhan.instaeclipse2.mods.extras.OnboardingPromptHook().install(dexKitBridge, hostClassLoader)` (166 ms)

Hooked (1):
- `X.0dcp#A09(android.content.Context,com.instagram.bloks.hosting.IgBloksScreenConfig)void`

Log:
```
   [OnboardingPromptHook.install:77] (InstaEclipse | Onboarding): ✅ hooked 1 launcher(s)
```

## NO-HOOK — NotificationRegistrationHook

`new io.github.somilkhan.instaeclipse2.mods.extras.NotificationRegistrationHook().install()` (51 ms)

## OK — StartupTabHook

`new io.github.somilkhan.instaeclipse2.mods.ui.StartupTabHook().install(hostClassLoader)` (52 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.feed.FollowingOnlyFeedHook().install(dexKitBridge, hostClassLoader)` (165 ms)

Hooked (5):
- `X.03dn#A01(android.content.Context,X.08Ci,X.0TXl,com.instagram.common.session.UserSession,X.03dA,X.02vf)void`
- `X.08Ci#A0L(java.lang.String,java.lang.String)void`
- `X.08Ci#AO3(java.lang.String,java.lang.String)void`
- `X.08Ci#APN(java.lang.String,java.lang.String)void`
- `X.08Ci#APe(java.lang.String,java.lang.String)void`

Log:
```
   [FollowingOnlyFeedHook.install:129] (InstaEclipse | FollowingFeed): ✅ hooked X.03dn.A01 + 4 adders
```

## OK — ProfileFollowsYouHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ProfileFollowsYouHook().install(dexKitBridge, hostClassLoader)` (134 ms)

Hooked (2):
- `com.instagram.profile.fragment.UserDetailFragment#A0m(com.instagram.profile.fragment.UserDetailFragment,boolean)void`
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [ProfileFollowsYouHook.install:98] (InstaEclipse | FollowsYou): ✅ Hooked A0m / followed_by A2K
```

## OK — PlaybackSpeedHook

`new io.github.somilkhan.instaeclipse2.mods.media.PlaybackSpeedHook().install(dexKitBridge, hostClassLoader)` (1078 ms)

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
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean)android.text.BoringLayout`
- `android.text.BoringLayout#replaceOrMake(java.lang.CharSequence,android.text.TextPaint,int,android.text.Layout$Alignment,float,float,android.text.BoringLayout$Metrics,boolean,android.text.TextUtils$TruncateAt,int)android.text.BoringLayout`

Log:
```
   [PlaybackSpeedHook.installFeed:509] (InstaEclipse | PlaybackSpeed): story/reel speed methods: 5
   [PlaybackSpeedHook.installFeed:510] (InstaEclipse | PlaybackSpeed): ✅ feed player X.06mS.A0O, started-cb X.06n0.Gj0, setters 1
   [PlaybackSpeedHook.installHero:266] (InstaEclipse | PlaybackSpeed): ✅ hero X.06r1.A0D, prepare A02
   [PlaybackSpeedHook.installReelScrubber:594] (InstaEclipse | PlaybackSpeed): ✅ reel scrubber gate X.0HbX.A00
   [PlaybackSpeedHook.installHoldSide:153] (InstaEclipse | PlaybackSpeed): ✅ hold side tracking
```

## OK — HideNotesTrayHook

`new io.github.somilkhan.instaeclipse2.mods.ui.HideNotesTrayHook().install(dexKitBridge)` (188 ms)

Hooked (1):
- `X.0BUT#invoke(java.lang.Object,java.lang.Object,java.lang.Object)java.lang.Object`

Log:
```
   [HideNotesTrayHook.installTaggedTray:112] (InstaEclipse | NotesTray): ✅ tagged tray X.0BUT.invoke
```

## PARTIAL — VoiceMessageDownloadHook

`new io.github.somilkhan.instaeclipse2.mods.media.VoiceMessageDownloadHook().install(dexKitBridge, hostClassLoader)` (693 ms)

Hooked (6):
- `X.0QCO#A00(X.0QCO,java.lang.String,java.lang.String,boolean)X.02wO`
- `com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel#A00(android.content.Context,android.graphics.PointF,android.graphics.Rect,com.instagram.aistudio.logging.AiStudioLoggingData,com.instagram.common.session.UserSession,X.02xK,com.instagram.model.direct.messageid.MessageIdentifier,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,java.util.List,java.util.List,int,int,long,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean)com.instagram.direct.messagethread.messageactions.model.MessageActionsViewModel`
- `X.0ecS#A08(android.app.Activity,android.view.View,com.instagram.common.session.UserSession,X.0mwp,X.0GfQ,X.0mo3,X.0muc,X.0mo5,X.0muw,X.0msr,X.0mvj,X.0moC,X.0moO,X.0GhL,X.0mp2,X.0Hav,X.0mph,X.0mpo,X.0mpp,X.0Gzs,X.0mpq,X.0mps,X.0mpt,X.0mpu,X.0mfm,X.0mui,X.0mpx,X.0muy,X.0Haw,X.0GlL,X.0Gzw,X.0GlN,X.0muk,X.0W1H,X.0aHO,X.02wO,com.instagram.model.direct.DirectThreadKey,X.02xK,com.instagram.model.direct.messageid.MessageIdentifier,X.0aiX,X.07GY,X.07GY,X.07GY,X.07GY,X.07GY,X.0ais,X.0gmh,X.0gmh,X.0gmh,X.0gmh,X.0gmh,X.0gmh,X.0atW,X.07GK,X.07GK,X.0aur,java.lang.String,java.lang.String,java.lang.String,java.util.List,kotlin.jvm.functions.Function0,int,long)void`
- `X.0fm5#A02(com.instagram.direct.messagethread.interaction.longpressaction.LongPressActionData,X.0fm5,java.lang.String)void`
- `X.0ZlE#EAl(X.0aHO,java.lang.String)void`
- `X.0go7#EAl(X.0aHO,java.lang.String)void`

Log:
```
!! [VoiceMessageDownloadHook.install:383] (IE|VoiceDL) ⚠️ compose rows: java.lang.reflect.InvocationTargetException / java.lang.UnsupportedOperationException: hook-verify stub: Instagram code does not run on the host JVM
   [VoiceMessageDownloadHook.install:385] (IE|VoiceDL) compose row hooks=0 (label field ok)
   [VoiceMessageDownloadHook.install:386] (IE|VoiceDL) ✅ lookups=1 factories=1 dispatchers=4 mapper=true message=X.02wO
   [BulkUnsend.install:142] (IE|BulkUnsend) ✅ unsend=X.07Hp.A0I ref=X.0ZMF
```

## OK — EditHistoryHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.EditHistoryHook().install(dexKitBridge, hostClassLoader)` (251 ms)

Hooked (2):
- `X.02wO#A19(com.instagram.common.session.UserSession,X.02wO,X.02yO)void`
- `X.02wO#A11()java.util.List`

Log:
```
   [EditHistoryHook.install:149] (IE|EditHistory) ✅ merge A19, history getter A11
```

## NO-HOOK — JumpToFirstMessageHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.JumpToFirstMessageHook().install()` (51 ms)

Log:
```
   [JumpToFirstMessageHook.install:51] (IE|JumpFirst) ✅ installed
```

## NO-HOOK — ReadReceiptChatHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.ReadReceiptChatHook().install(hostClassLoader)` (51 ms)

Log:
```
   [ReadReceiptChatHook.install:51] (IE|ReadReceiptChat) ✅ installed
```

## OK — StoryViewerSearchHook

`new io.github.somilkhan.instaeclipse2.mods.ui.StoryViewerSearchHook().install(dexKitBridge, hostClassLoader)` (312 ms)

Hooked (1):
- `X.0TVX#A07(java.util.List,boolean,boolean,boolean)void`

Log:
```
   [StoryViewerSearchHook.install:103] (IE|ViewerSearch) ✅ adapter X.0TVX.A07 rebuild=A14
```

## OK — CommentSearchHook

`new io.github.somilkhan.instaeclipse2.mods.misc.CommentSearchHook().install(dexKitBridge, hostClassLoader)` (485 ms)

Hooked (2):
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#A07(X.0Cwz,X.0Cwy,X.0Cwy,X.0Cxi,X.0D3l,X.0DBq,X.0DBr,X.0Cvj,X.0Cwv,X.0D7l,com.instagram.comments.mvvm.viewmodel.CommentListViewModel,X.0Nxz,X.0DBs,X.0Gom,boolean,boolean,boolean)X.0ejk`
- `X.0M5A#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [CommentSearchHook.install:130] (IE|CommentSearch) comment json: serializer=true factory=true
   [CommentSearchHook.install:151] (IE|CommentSearch) ✅ vm com.instagram.comments.mvvm.viewmodel.CommentListViewModel builders=1 fragments=1
```

## OK — ChatExportHook

`new io.github.somilkhan.instaeclipse2.mods.ghost.ChatExportHook().install(dexKitBridge, hostClassLoader)` (354 ms)

Hooked (1):
- `X.03GI#<init>(com.instagram.common.session.UserSession,X.02yB,java.util.List)`

Log:
```
   [ChatExportHook.install:124] (IE|ChatExport) ✅ store X.03GI messages=true serializer=true factory=X.02az
```

## OK — FollowListExportHook

`new io.github.somilkhan.instaeclipse2.mods.misc.FollowListExportHook().install(dexKitBridge, hostClassLoader)` (348 ms)

Hooked (1):
- `X.0T5x#onViewCreated(android.view.View,android.os.Bundle)void`

Log:
```
   [FollowListExportHook.findPager:232] (IE|FollowExport) pager=X.0CqI loadMore=AXk
   [FollowListExportHook.install:106] (IE|FollowExport) ✅ X.0T5x fullName=true
```

## OK — LikeUndoHook

`new io.github.somilkhan.instaeclipse2.mods.misc.LikeUndoHook().install(dexKitBridge)` (296 ms)

Hooked (1):
- `X.0Sm8#A03(android.content.Context,X.09rh,com.instagram.common.session.UserSession,X.0Whp,com.instagram.feed.media.Media,X.01Yz,com.instagram.search.common.analytics.SearchContext,java.lang.Double,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Long,java.lang.Long,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.Map,boolean)void`

Log:
```
   [LikeUndoHook.install:130] (InstaEclipse | LikeUndo): ✅ Hooked X.0Sm8.A03
```

## OK — ShareSheetCleanupHook

`new io.github.somilkhan.instaeclipse2.mods.ui.ShareSheetCleanupHook().install(hostClassLoader)` (58 ms)

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

`new DisableStoryFlippingHook().handleStoryFlippingDisable(dexKitBridge)` (154 ms)

Hooked (1):
- `instagram.features.stories.fragment.ReelViewerFragment#A1h(com.instagram.model.reels.ReelItem)void`

Log:
```
   [DisableStoryFlippingHook.findAndHookMethod:84] (InstaEclipse | StoryFlipping): ✅ Hooked (dynamic check): instagram.features.stories.fragment.ReelViewerFragment.A1h
```

## OK — StoryMentionHook

`new StoryMentionHook().install(dexKitBridge, lpparam.classLoader)` (639 ms)

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
   [StoryMentionHook.resolveMentionPipeline:154] (IE|Mention) ✅ pipeline resolved: A9B -> A0E
   [StoryMentionHook.installButtonHook:232] (IE|Mention) ✅ button hook installed on 3 builder(s)
   [StoryMentionHook.installClickHook:287] (IE|Mention) ✅ click hook installed
```

## OK — StoryLinksHook

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryLinksHook().install(dexKitBridge, lpparam.classLoader)` (361 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.misc.StoryInfoHook().install(dexKitBridge, lpparam.classLoader)` (1548 ms)

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

`new CommentCopyHook().install(dexKitBridge, lpparam.classLoader)` (227 ms)

Hooked (4):
- `X.0M5A#onViewCreated(android.view.View,android.os.Bundle)void`
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `com.instagram.comments.mvvm.viewmodel.CommentListViewModel#GP6(java.lang.String,java.lang.String,float,boolean)void`

Log:
```
   [CommentCopyHook.findAndHookNew:198] (InstaEclipse | CopyComment): ✅ Hooked com.instagram.comments.mvvm.viewmodel.CommentListViewModel.GP6 (resolver=X.0DCn.A01)
```

## OK — CaptionCopyContextMenuHook

`new CaptionCopyContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (1233 ms)

Hooked (11):
- `android.app.Activity#onResume()void`
- `android.app.Activity#onDestroy()void`
- `X.0diz#A00(X.03bC,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0diz,X.0VWq,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.0Ckv#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Ckv#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Mgn#A09(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0cnG#A01(boolean)java.util.List`
- `X.0Ckr#A06(X.0BX4,X.075A,X.0Ckr)java.util.ArrayList`
- `X.0Ckw#A01(com.instagram.common.session.UserSession,com.instagram.feed.media.mediaoption.MediaOption$Option,boolean)int`
- `X.0Zwh#A00(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,X.0RNw,X.0kyJ,java.lang.Integer,java.lang.String,java.lang.String,java.lang.String,float,boolean)void`
- `X.0Zwh#A01(android.content.Context,android.view.View$OnClickListener,android.view.View$OnClickListener,java.lang.String,java.lang.String,float,int,int,boolean,boolean,boolean,boolean,boolean)void`

Log:
```
   [CaptionCopyContextMenuHook.resolveCaptionGetter:221] (IE|Caption) ✅ captionGetter=com.instagram.feed.media.Media.A32
   [CaptionCopyContextMenuHook.installAddButtonHook:487] (IE|Caption) ✅ Caption copy menu hook installed
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:755] (IE|Caption) ✅ reel label override on X.0Zwh.A00
   [CaptionCopyContextMenuHook.installReelLabelOverrideHook:755] (IE|Caption) ✅ reel label override on X.0Zwh.A01
```

## OK — DisableDoubleTapLikeHook

`new DisableDoubleTapLikeHook().install(dexKitBridge, lpparam.classLoader)` (783 ms)

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

`new io.github.somilkhan.instaeclipse2.mods.misc.DoubleTapExtrasHook().install(dexKitBridge, lpparam.classLoader)` (329 ms)

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

`new IgThemeHook().install(hostClassLoader)` (62 ms)

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

`new ForceReelQualityHook().install(dexKitBridge, lpparam.classLoader)` (111 ms)

Hooked (1):
- `com.instagram.feed.media.Media#AAM()java.util.List`

Log:
```
   [ForceReelQualityHook.install:129] (InstaEclipse | ForceReelQuality): ✅ Hooked com.instagram.feed.media.Media#AAM (height=CHg)
```

## OK — MaxUploadQualityHook

`new io.github.somilkhan.instaeclipse2.mods.media.MaxUploadQualityHook().install(dexKitBridge, lpparam.classLoader)` (440 ms)

Hooked (1):
- `X.0BWS#A00(X.02nf)boolean`

Log:
```
   [MaxUploadQualityHook.install:73] (IE|MaxUploadQuality) ✅ hooked 1 method(s)
```

## OK — DisableVideoAutoPlayHook

`new DisableVideoAutoPlayHook().handleAutoPlayDisable(dexKitBridge)` (173 ms)

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

`new BuildExpiredPopupHook().install(dexKitBridge, lpparam.classLoader)` (282 ms)

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
   [FeedVideoDownloadHook.install:187] (IE|DL) media model: mutable=false liveTree=false listCandidates=0
```

## OK — FeedVideoDownloadHook

`FeedVideoDownloadHook.installVideoUrlCaptureHook(dexKitBridge, lpparam.classLoader)` (2244 ms)

Hooked (2):
- `com.instagram.api.schemas.ImmutablePandoVideoVersion#getUrl()java.lang.String`
- `com.instagram.api.schemas.VideoVersion#getUrl()java.lang.String`

Log:
```
   [SelectedMediaAdapter.install:61] (IE|MediaAdapter) resolved properties=13 manifest=true direct=true story=true
   [FeedVideoDownloadHook.discoverDynamicMediaModel:1277] (IE|DL|DexKit) dynamic media dict=X.09AK
   [FeedVideoDownloadHook.resolveVideoVersionsGetters:1333] (IE|DL|DexKit) video_versions getters=1
   [FeedVideoDownloadHook.resolveCarouselGetter:1378] (IE|DL|DexKit) carousel getter=com.instagram.feed.media.Media.A8U
   [FeedVideoDownloadHook.resolveDashManifestGetter:1447] (IE|DL|DexKit) dash manifest getter=com.instagram.feed.media.Media.A89
   [FeedVideoDownloadHook.resolveHasAudioGetter:1405] (IE|DL|DexKit) has_audio getter=A3l
   [FeedVideoDownloadHook.resolveMediaGetter:1640] (IE|DL|DexKit) music_metadata getter=A29
   [FeedVideoDownloadHook.resolveMediaGetter:1640] (IE|DL|DexKit) clips_metadata getter=A2y
   [FeedVideoDownloadHook.resolveIsVideoMethod:1780] (IE|DL|DexKit) isVideo: no Media ()Z getter (457+ layout), using video_versions presence
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1203] (IE|DL|DexKit) VideoVersionIntf implementors found: 2
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1219] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.ImmutablePandoVideoVersion
   [FeedVideoDownloadHook.installVideoUrlCaptureHook:1219] (IE|DL|DexKit) ✅ Hooked getUrl() on com.instagram.api.schemas.VideoVersion
   [FeedVideoDownloadHook.resolveUsernameGetter:1827] (IE|DL|Username) userClass=com.instagram.user.model.User
   [FeedVideoDownloadHook.resolveUsernameGetter:1868] (IE|DL|Username) userUsernameGetter=A7i (excluded 0 full_name getter(s))
   [FeedVideoDownloadHook.resolveMediaAuthorGetter:1913] (IE|DL|Username) ✅ mediaAuthorGetter (Media."user"): A3M
   [FeedVideoDownloadHook.resolveDictUserGetter:1992] (IE|DL|Username) dictUserGetter: no dict author getter (446+ layout), using Media.A3M
```

## OK — PostDownloadContextMenuHook

`new PostDownloadContextMenuHook().install(dexKitBridge, lpparam.classLoader)` (361 ms)

Hooked (5):
- `X.0diz#A00(X.03bC,com.instagram.feed.media.mediaoption.MediaOption$Option,X.0diz,X.0VWq,java.lang.CharSequence,java.util.ArrayList,boolean)void`
- `X.0Ckv#A0g(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Ckv#A0h(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0Mgn#A09(com.instagram.feed.media.mediaoption.MediaOption$Option)void`
- `X.0cnG#A01(boolean)java.util.List`

Log:
```
   [PostDownloadContextMenuHook.installAddButtonHook:338] (IE|Post) ✅ Post menu inject hook installed
   [PostDownloadContextMenuHook.resolveReelClickDispatcher:381] (IE|Post) selected Reel dispatcher bound=false
   [PostDownloadContextMenuHook.installAllowlistPatchHook:535] (IE|Post) ✅ Allowlist patch hooked: X.0cnG.A01
```

## OK — InstantSaveHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantSaveHook().install(lpparam.classLoader)` (55 ms)

Hooked (2):
- `com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1#<init>(androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,androidx.compose.runtime.MutableState,X.0X94,com.instagram.common.session.UserSession,com.instagram.compose.ui.emitter.AnimatedParticleEffectState,X.03Et,java.util.List,java.util.List,X.0UAG,int,boolean)`
- `com.instagram.quicksnap.viewer.compose.PointerTouchKt$handleTapAndLongPressWithRelease$2$1$1$1#<init>(X.0UAG,kotlin.jvm.functions.Function0,kotlin.jvm.functions.Function1,kotlin.jvm.functions.Function1,float,long,boolean)`

Log:
```
   [InstantSaveHook.cacheCurrentInstant:77] (IE|InstantSave) ✅ viewer cache hooked: com.instagram.quicksnap.viewer.compose.QuickSnapMediaViewerScreenKt$QuickSnapMediaViewerScreen$30$1$1$21$1
   [InstantSaveHook.wrapLongPress:183] (IE|InstantSave) ✅ long-press hooked
```

## OK — InstantUploadHook

`new io.github.somilkhan.instaeclipse2.mods.media.InstantUploadHook().install(lpparam.classLoader)` (59 ms)

Hooked (10):
- `com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel#A1C(android.content.Context,android.graphics.Bitmap,X.07aN,X.0SYs,X.0TL7,X.0ZCd,java.lang.Long,java.lang.String,java.lang.String,X.0UAG,X.02oc,X.0mym,float,long,boolean)java.lang.Object`
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
   [InstantUploadHook.hookSwap:118] (IE|InstantUpload) ✅ capture handler hooked
   [InstantUploadHook.hookCameraOpen:239] (IE|InstantUpload) ✅ camera-open hooked (8 VM methods)
   [InstantUploadHook.hookPickerResult:686] (IE|InstantUpload) ✅ picker-result hooked
```

## OK — GhostEphemeralKeepHook

`new GhostEphemeralKeepHook().install(dexKitBridge, lpparam.classLoader)` (413 ms)

Hooked (6):
- `X.03GI#A0b(boolean)boolean`
- `X.03GI#A0Z()boolean`
- `X.06ec#HCv(X.02sx,X.0XoP,X.0ArI)void`
- `X.0005#A00(int)java.lang.String`
- `X.02wQ#unsafeParseFromJson(X.088A)java.lang.Object`
- `X.02wQ#parseFromJsonParser(X.088A)java.lang.Object`

Log:
```
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.03GI.A0b
   [GhostEphemeralKeepHook.hookVanishLocalDelete:112] (IE|Ephemeral) ✅ local purge hook → X.03GI.A0Z
   [GhostEphemeralKeepHook.hookServerPing:187] (IE|Ephemeral) ✅ server-ping hook → X.06ec.HCv
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.0005.A00
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.02wQ.unsafeParseFromJson
   [GhostEphemeralKeepHook.hookExpiryParser:253] (IE|Ephemeral) ✅ expiry-parser hook → X.02wQ.parseFromJsonParser
```

## OK — GhostPermanentViewHook

`new GhostPermanentViewHook().install(dexKitBridge, lpparam.classLoader)` (175 ms)

Hooked (1):
- `X.0000#A00(int)java.lang.String`

Log:
```
   [GhostPermanentViewHook.install:96] (IE|ViewOnceMedia) ✅ hooking X.0000.A00
   [GhostPermanentViewHook.install:103] (IE|ViewOnceMedia) ✅ hooked
```

## OK — ViewOnceBadgeHook

`new ViewOnceBadgeHook().install(dexKitBridge, lpparam.classLoader)` (79 ms)

Hooked (1):
- `X.08XG#<init>(X.02HI,X.02GX,X.08XF,com.instagram.direct.thread.analytics.DirectThreadAnalyticsParams,com.instagram.direct.wellbeing.harmfulcontent.ondevicenudity.view.data.PrivacyMediaOverlayViewModel,X.0AHT,java.lang.String,java.lang.String,boolean,boolean,boolean,boolean)`

Log:
```
   [ViewOnceBadgeHook.install:75] (IE|VOBadge) ✅ hooked X.08XG.<init>
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

`new io.github.somilkhan.instaeclipse2.mods.ui.SavedOnProfileHook().install(dexKitBridge, hostClassLoader)` (57 ms)

Hooked (1):
- `androidx.fragment.app.Fragment#onResume()void`

Log:
```
   [SavedOnProfileHook.install:128] (IE|SavedProfile) ✅ X.0P5K.A00 / public static X.04MO X.01X3.A0Y(androidx.fragment.app.FragmentActivity,X.01xo)
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

`new io.github.somilkhan.instaeclipse2.mods.media.ShareLinkDownloadHook().install(hostClassLoader)` (52 ms)

Hooked (3):
- `com.instagram.common.session.UserSession#<init>(X.08Jf,java.lang.String,X.02as,boolean,boolean)`
- `android.app.Activity#onCreate(android.os.Bundle)void`
- `android.app.Activity#onNewIntent(android.content.Intent)void`

Log:
```
   [ShareLinkDownloadHook.install:71] (IE|ShareDL) ✅ installed
```

## OK — StoryDownloadHook

`new StoryDownloadHook().install(dexKitBridge, lpparam.classLoader)` (1049 ms)

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
   [StoryDownloadHook.install:192] (IE|Highlight) reel class X.04ot
   [StoryDownloadHook.installButtonInjectorHook:599] (IE|Story) button injector hooked 3 builder(s)
   [StoryDownloadHook.installHighlightButtonInjectorHook:664] (IE|Story) Highlight injector hooked 2 builder(s)
   [StoryDownloadHook.installClickHandlerHook:798] (IE|Story) click handler hooked 6 dispatcher(s)
   [StoryDownloadHook.installHighlightTrayHook:290] (IE|HighlightTray) ✅ X.0O1f legacy=true popup=false loader=false
   [StoryDownloadHook.installStoryCaptureHook:448] (IE|StoryCache) capture hook: 1 method(s)
```

## OK — ReelDownloadHook

`new ReelDownloadHook().install(dexKitBridge, lpparam.classLoader)` (485 ms)

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

`interceptor.handleInterceptor(lpparam)` (75 ms)

Hooked (2):
- `com.instagram.api.tigon.TigonServiceLayer#startRequest(X.03wd,X.03wu,X.04kA)X.0A0F`
- `X.01xv#startRequest(X.03wd,X.03wu,X.04kA)X.0A0F`

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

