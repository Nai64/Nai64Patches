package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.fireHiddenCallbacks
import java.util.logging.Logger

@Suppress("unused")
val noAdsPatch = bytecodePatch(
    name = "No Ads",
    description = "Remove ads",
    default = false,
) {
    val blockInterstitials by booleanOption(
        title = "Block Interstitials",
        default = true,
        key = "blockInterstitials",
        description = "",
    )
    val blockBanners by booleanOption(
        title = "Block Banners",
        default = true,
        key = "blockBanners",
        description = "",
    )
    val blockAppOpen by booleanOption(
        title = "Block App Open",
        default = true,
        key = "blockAppOpen",
        description = "",
    )
    val blockMRec by booleanOption(
        title = "Block MREC",
        default = true,
        key = "blockMRec",
        description = "",
    )
    val blockRewarded by booleanOption(
        title = "Block Rewarded",
        default = true,
        key = "blockRewarded",
        description = "",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

val hasMaxUnity = ShowInterstitialFingerprint.methodOrNull != null ||
            ShowAppOpenAdFingerprint.methodOrNull != null ||
            ShowBannerFingerprint.methodOrNull != null
        val hasNativeMax = MaxInterstitialAdShowAdFingerprint.methodOrNull != null ||
            MaxAppOpenAdShowAdFingerprint.methodOrNull != null ||
            MaxAdViewStartAutoRefreshFingerprint.methodOrNull != null
        val hasAdMob = AdMobInterstitialShowFingerprint.methodOrNull != null ||
            AdMobLegacyInterstitialShowFingerprint.methodOrNull != null ||
            AdMobAppOpenShowFingerprint.methodOrNull != null ||
            AdMobRewardedShowFingerprint.methodOrNull != null ||
            AdMobLegacyRewardedVideoShowFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax && !hasAdMob) {
            logger.warning("Could not find supported ad SDK (MAX Unity, native MAX or AdMob). No changes applied.")
            return@execute
        }

        // ── MAX Unity wrapper ──
        if (blockInterstitials == true) {
            ShowInterstitialFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockAppOpen == true) {
            ShowAppOpenAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockBanners == true) {
            ShowBannerFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            StartBannerAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockMRec == true) {
            ShowMRecFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            StartMRecAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── Native MAX (non-Unity) ──
        if (blockInterstitials == true) {
            MaxInterstitialAdShowAdFingerprint.methodOrNull?.let {
                it.addInstructions(0, fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxInterstitialAd;"))
            }
        }
        if (blockAppOpen == true) {
            MaxAppOpenAdShowAdFingerprint.methodOrNull?.let {
                it.addInstructions(0, fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxAppOpenAd;"))
            }
        }
        if (blockBanners == true || blockMRec == true) {
            MaxAdViewStartAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── AdMob (Google Mobile Ads) ──
        if (blockInterstitials == true) {
            AdMobInterstitialShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AdMobLegacyInterstitialShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockAppOpen == true) {
            AdMobAppOpenShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockRewarded == true) {
            AdMobRewardedShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AdMobLegacyRewardedVideoShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── Rewarded ads ──
        if (blockRewarded == true) {
            IsRewardedAdReadyFingerprint.methodOrNull?.let {
                it.addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent())
            }
            ShowRewardedAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            MaxRewardedAdIsReadyFingerprint.methodOrNull?.let {
                it.addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent())
            }
            MaxRewardedAdShowAdFingerprint.methodOrNull?.let {
                it.addInstructions(0, fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxRewardedAd;"))
            }
        }
    }
}
