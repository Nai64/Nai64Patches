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

// ── Native MAX (non-Unity) fingerprints ──

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
