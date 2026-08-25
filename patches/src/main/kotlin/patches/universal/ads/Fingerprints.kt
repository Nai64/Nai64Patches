package patches.universal.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShowRewardedAdFingerprint : Fingerprint(
    name = "showRewardedAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
)

internal object LoadRewardedAdFingerprint : Fingerprint(
    name = "loadRewardedAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object IsRewardedAdReadyFingerprint : Fingerprint(
    name = "isRewardedAdReady",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
)

internal object MaxUnityAdManagerOnAdHiddenFingerprint : Fingerprint(
    name = "onAdHidden",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Lcom/applovin/mediation/MaxAd;"),
)

internal object ShowInterstitialFingerprint : Fingerprint(
    name = "showInterstitial",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
)

internal object ShowAppOpenAdFingerprint : Fingerprint(
    name = "showAppOpenAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
)

internal object ShowBannerFingerprint : Fingerprint(
    name = "showBanner",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object ShowMRecFingerprint : Fingerprint(
    name = "showMRec",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object StartBannerAutoRefreshFingerprint : Fingerprint(
    name = "startBannerAutoRefresh",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object StartMRecAutoRefreshFingerprint : Fingerprint(
    name = "startMRecAutoRefresh",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object PerformLocalInstallerCheckFingerprint : Fingerprint(
    name = "performLocalInstallerCheck",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = emptyList(),
)

// -- Generic Play Store installer check fingerprints --
// Match methods containing "com.android.vending" string in their body,
// a strong indicator of install source checking logic.

internal object GenericBooleanInstallerCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("com.android.vending"),
)

internal object GenericStringInstallerCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("com.android.vending"),
)

internal object FallbackBooleanInstallerCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("com.android.vending"),
)

internal object FallbackStringInstallerCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("com.android.vending"),
)

// -- Amazon Appstore availability fingerprints --
// These target app-specific checks that directly reference an Amazon
// Appstore package name. They do not modify PackageManager globally.

internal object AmazonVeneziaBooleanAvailabilityFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("com.amazon.venezia"),
)

internal object AmazonVeneziaStringAvailabilityFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("com.amazon.venezia"),
)

internal object AmazonMarketplaceBooleanAvailabilityFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("com.amazon.device.marketplace"),
)

internal object AmazonMarketplaceStringAvailabilityFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("com.amazon.device.marketplace"),
)

internal object PairipSignatureCheckVerifyIntegrityFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object PairipSignatureCheckVerifySignatureMatchesFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifySignatureMatches",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
)

internal object PairipLicenseClientStartErrorDialogFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startErrorDialogActivity",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseClientStartPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "startPaywallActivity",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Landroid/app/PendingIntent;"),
)

internal object PairipLicenseActivityShowPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "showPaywallAndCloseApp",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityNnStartFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "nnStart",
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityOnStartFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "onStart",
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityCloseAppFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "closeApp",
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityExitAppFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "exitApp",
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityCloseappFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "closeapp",
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipLicenseActivityExitappFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "exitapp",
    returnType = "V",
    parameters = emptyList(),
)

// -- Pairip Application class bypass --
// Prevents Pairip from loading libpairipcore.so and starting the VM
// by neutering the Application entry point methods.

internal object PairipApplicationAttachBaseContextFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/application/Application;",
    name = "attachBaseContext",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object PairipApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/application/Application;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)

// -- Pairip runtime interception strategies --

internal object PairipApplicationClinitFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/application/Application;",
    name = "<clinit>",
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "V",
    parameters = emptyList(),
)

internal object PairipStartupLauncherLaunchFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    returnType = "V",
)

internal object PairipStartupLauncherPairipFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "pairip",
    returnType = "V",
)

internal object PairipVMRunnerInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
)

internal object PairipLicenseClientV3OnActivityCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck3/LicenseClientV3;",
    name = "onActivityCreate",
    returnType = "V",
)

// -- Native MAX (non-Unity) fingerprints --

internal object MaxRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
)

internal object MaxRewardedAdShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
)

internal object MaxInterstitialAdShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "showAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
)

internal object MaxAppOpenAdShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAppOpenAd;",
    name = "showAd",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
)

internal object MaxAdViewStartAutoRefreshFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAdView;",
    name = "startAutoRefresh",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
)

// -- Unity Ads RewardedAd fingerprints --

internal object UnityRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/RewardedAd;",
    name = "show",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/unity3d/ads/ShowConfiguration;",
        "Lcom/unity3d/ads/RewardedShowListener;",
    ),
)

// -- Unity Ads SDK v4 fingerprints --
// New Unity Ads 4.x API: UnityAds.show(Activity, placementId, listener) fires
// IUnityAdsShowListener.onUnityAdsShowComplete(placementId, completionState).

internal object UnityAdsV4Show3ArgFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Lcom/unity3d/ads/IUnityAdsShowListener;",
    ),
)

internal object UnityAdsV4Show4ArgFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Lcom/unity3d/ads/UnityAdsShowOptions;",
        "Lcom/unity3d/ads/IUnityAdsShowListener;",
    ),
)

// -- Unity Ads v3 (legacy) --
// Older Unity Ads API: UnityAds.show(Activity, placementId) without listeners.

internal object UnityAdsV3Show2ArgFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
    ),
)

internal object UnityAdsV3ShowOptionsFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Lcom/unity3d/ads/UnityAdsShowOptions;",
    ),
)

// -- VK MyTarget / RuStore distribution ad fingerprints --
// MyTarget's InterstitialAd and RewardedAd inherit show(Context) from the
// common BaseInterstitialAd implementation.
internal object MyTargetBaseInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/my/target/ads/BaseInterstitialAd;",
    name = "show",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object YandexUnityRewardedWrapperShowFingerprint : Fingerprint(
    definingClass = "Lcom/yandex/mobile/ads/unity/wrapper/rewarded/RewardedAdWrapper;",
    name = "show",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object YandexUnityRewardedListenerOnRewardedFingerprint : Fingerprint(
    definingClass = "Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;",
    name = "onRewarded",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Lcom/yandex/mobile/ads/rewarded/Reward;"),
)

internal object YandexUnityInterstitialWrapperShowFingerprint : Fingerprint(
    definingClass = "Lcom/yandex/mobile/ads/unity/wrapper/interstitial/InterstitialAdWrapper;",
    name = "show",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object YandexMyTargetRewardedIsLoadedFingerprint : Fingerprint(
    definingClass = "Lcom/yandex/mobile/ads/mediation/mytarget/m;",
    name = "isLoaded",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

internal object YandexMyTargetInterstitialIsLoadedFingerprint : Fingerprint(
    definingClass = "Lcom/yandex/mobile/ads/mediation/mytarget/c0;",
    name = "isLoaded",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

// -- ironSource (LevelPlay) public API --

internal object IronSourceShowDemandOnlyInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showISDemandOnlyInterstitial",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object IronSourceShowDemandOnlyRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showISDemandOnlyRewardedVideo",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object IronSourceShowInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showInterstitial",
    returnType = "V",
    parameters = emptyList(),
)

internal object IronSourceShowInterstitialActivityFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showInterstitial",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object IronSourceShowInterstitialActivityPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showInterstitial",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;"),
)

internal object IronSourceShowInterstitialPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showInterstitial",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

internal object IronSourceShowRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    returnType = "V",
    parameters = emptyList(),
)

internal object IronSourceShowRewardedVideoActivityFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object IronSourceShowRewardedVideoActivityPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;"),
)

internal object IronSourceShowRewardedVideoPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

// -- AppLovin legacy (direct SDK, non-MAX) --

internal object AppLovinInterstitialDialogShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/adview/AppLovinInterstitialAdDialog;",
    name = "show",
    returnType = "V",
    parameters = emptyList(),
)

internal object AppLovinInterstitialDialogShowAndRenderFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/adview/AppLovinInterstitialAdDialog;",
    name = "showAndRender",
    returnType = "V",
    parameters = listOf("Lcom/applovin/sdk/AppLovinAd;"),
)

internal object AppLovinIncentivizedShow4ListenerFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/adview/AppLovinIncentivizedInterstitial;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/applovin/sdk/AppLovinAdRewardListener;",
        "Lcom/applovin/sdk/AppLovinAdVideoPlaybackListener;",
        "Lcom/applovin/sdk/AppLovinAdDisplayListener;",
    ),
)

internal object AppLovinIncentivizedShow5ListenerFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/adview/AppLovinIncentivizedInterstitial;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/applovin/sdk/AppLovinAdRewardListener;",
        "Lcom/applovin/sdk/AppLovinAdVideoPlaybackListener;",
        "Lcom/applovin/sdk/AppLovinAdDisplayListener;",
        "Lcom/applovin/sdk/AppLovinAdClickListener;",
    ),
)

internal object AppLovinAdViewLoadNextAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/adview/AppLovinAdView;",
    name = "loadNextAd",
    returnType = "V",
    parameters = emptyList(),
)

// -- Vungle --
// Vungle 7.x: VungleInterstitialAd / VungleRewardedAd both load through
// BaseFullscreenAd.load(placementId). Blocking the load makes shows no-op.

internal object VungleBaseFullscreenAdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/vungle/ads/BaseFullscreenAd;",
    name = "load",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

// -- Meta Audience Network (facebook/ads) --
// show() returns Z in the modern AN SDK.

internal object FacebookInterstitialAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/InterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = emptyList(),
)

internal object FacebookInterstitialAdShowConfigFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/InterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = listOf("Lcom/facebook/ads/InterstitialAd\$InterstitialShowAdConfig;"),
)

internal object FacebookRewardedVideoAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/RewardedVideoAd;",
    name = "show",
    returnType = "Z",
    parameters = emptyList(),
)

internal object FacebookRewardedVideoAdShowConfigFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/RewardedVideoAd;",
    name = "show",
    returnType = "Z",
    parameters = listOf("Lcom/facebook/ads/RewardedVideoAd\$RewardedVideoShowAdConfig;"),
)

internal object FacebookRewardedInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/RewardedInterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = emptyList(),
)

internal object FacebookRewardedInterstitialShowConfigFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/RewardedInterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = listOf("Lcom/facebook/ads/RewardedInterstitialAd\$RewardedInterstitialShowAdConfig;"),
)

// -- Pangle (bytedance) --

internal object PangleInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/sdk/openadsdk/api/interstitial/PAGInterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object PangleRewardedShowFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/sdk/openadsdk/api/reward/PAGRewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object PangleAppOpenShowFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/sdk/openadsdk/api/open/PAGAppOpenAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

// -- LevelPlay RewardedAd fingerprints --

internal object LevelPlayRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
    name = "isAdReady",
    returnType = "Z",
    parameters = emptyList(),
)

internal object IronSourceUnityRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "isAdReady",
    returnType = "Z",
    parameters = emptyList(),
)

internal object IronSourceLevelPlayFullScreenShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/Ya;",
    name = "a",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
    ),
)

// -- Play In-app Updates --

internal object AppUpdateManagerImplStartUpdateFlowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/play/core/appupdate/AppUpdateManagerImpl;",
    name = "startUpdateFlowForResult",
    returnType = "Lcom/google/android/play/core/tasks/Task;",
)

// -- RootBeer root detection --

internal object RootBeerIsRootedFingerprint : Fingerprint(
    definingClass = "Lcom/scottyab/rootbeer/RootBeer;",
    name = "isRooted",
    returnType = "Z",
)

internal object RootBeerDetectSuBinaryFingerprint : Fingerprint(
    definingClass = "Lcom/scottyab/rootbeer/RootBeer;",
    name = "detectSuBinary",
    returnType = "Z",
)

internal object RootBeerDetectTestKeysFingerprint : Fingerprint(
    definingClass = "Lcom/scottyab/rootbeer/RootBeer;",
    name = "detectTestKeys",
    returnType = "Z",
)

// -- RevenueCat --

internal object EntitlementInfoIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/EntitlementInfo;",
    name = "isActive",
    returnType = "Z",
    parameters = emptyList(),
)

// -- Pairip LicenseClient.checkLicense root kill --

internal object PairipLicenseClientCheckLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object PairipLicenseClientInitializeLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    returnType = "V",
    parameters = emptyList(),
)

// -- Pairip LicenseContentProvider lifecycle neuter --

internal object PairipLicenseContentProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseContentProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = emptyList(),
)

internal object PairipLicenseContentProviderQueryFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseContentProvider;",
    name = "query",
    returnType = "Landroid/database/Cursor;",
    parameters = listOf(
        "Landroid/net/Uri;",
        "[Ljava/lang/String;",
        "Ljava/lang/String;",
        "[Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

// -- Pairip InitContextProvider --

internal object PairipInitContextProviderGetContextFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/InitContextProvider;",
    name = "getContext",
    returnType = "Landroid/content/Context;",
    parameters = emptyList(),
)

// -- Pairip LicenseResponseHelper (response validation sink) --

internal object PairipLicenseResponseHelperValidateResponseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseResponseHelper;",
    name = "validateResponse",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
)

internal object PairipLicenseResponseHelperGetRepeatedCheckMetadataFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseResponseHelper;",
    name = "getRepeatedCheckMetadata",
    returnType = "Landroid/os/Bundle;",
    parameters = listOf("Landroid/os/Bundle;"),
)

internal object PairipLicenseResponseHelperVerifySignatureFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseResponseHelper;",
    name = "verifySignature",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/security/PublicKey;"),
)

// -- Pairip ResponseValidator (older response validation path) --

internal object PairipResponseValidatorValidateResponseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/ResponseValidator;",
    name = "validateResponse",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
)

internal object PairipResponseValidatorVerifySignatureFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/ResponseValidator;",
    name = "verifySignature",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/security/PublicKey;"),
)

// -- Pairip licensecheck3 ResponseValidator (V3 response validation path) --

internal object PairipResponseValidatorV3ValidateResponseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck3/ResponseValidator;",
    name = "validateResponse",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
)

// -- Pairip V2 (newer licensecheck runtime) --
// Targets the V2 license client shipped in newer Pairip-protected apps.
// Method prototypes were dumped directly from a protected APK.

internal object PairipV2CheckLicenseInternalFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicenseInternal",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Landroid/os/IBinder;"),
)

internal object PairipV2LicenseResponseHelperVerifySignatureFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseResponseHelper;",
    name = "verifySignature",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/security/PublicKey;",
    ),
)

internal object PairipV2ScheduleRepeatedLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "scheduleRepeatedLicenseCheck",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("Lcom/pairip/licensecheck/RepeatedCheckMetadata;"),
)

// AdMob (Google Mobile Ads)

internal object AdMobInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object AdMobLegacyInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/InterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = emptyList(),
)

internal object AdMobAppOpenShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/appopen/AppOpenAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object AdMobAppOpenLoadFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/appopen/AppOpenAd;",
    name = "load",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Lcom/google/android/gms/ads/AdRequest;",
        "Lcom/google/android/gms/ads/appopen/AppOpenAd\$AppOpenAdLoadCallback;",
    ),
)

internal object AdMobRewardedShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/rewarded/RewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;"),
)

internal object AdMobLegacyRewardedVideoShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/reward/RewardedVideoAd;",
    name = "show",
    returnType = "V",
    parameters = emptyList(),
)

// -- Ad "available / ready" gates (Fake Ad Availability patch) --
// These report whether an ad can currently be shown. Forcing them to
// return true makes the game's UI treat ads as available, so it proceeds
// to call show() and the existing Ads Free Rewards instant-reward hooks fire.
// All are matched opportunistically (methodOrNull); a wrong guess simply
// does not apply on a given app.

// Unity Ads SDK: Advertisement.isReady() / isReady(String)
internal object UnityAdsAdvertisementIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/Advertisement;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
)

internal object UnityAdsAdvertisementIsReadyPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/Advertisement;",
    name = "isReady",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
)

// Unity Ads SDK: UnityAds.isReady() (legacy static entry point)
internal object UnityAdsSdkIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
)

// ironSource / LevelPlay: isRewardedVideoAvailable / isInterstitialReady
internal object IronSourceIsRewardedVideoAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isRewardedVideoAvailable",
    returnType = "Z",
    parameters = emptyList(),
)

internal object IronSourceIsInterstitialReadyFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isInterstitialReady",
    returnType = "Z",
    parameters = emptyList(),
)

// AppLovin MAX: interstitial / app-open readiness
internal object MaxInterstitialAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
)

internal object MaxAppOpenAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAppOpenAd;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
)
