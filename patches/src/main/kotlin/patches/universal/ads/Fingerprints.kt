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
