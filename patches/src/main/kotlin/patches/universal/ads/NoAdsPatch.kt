package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.fireHiddenCallbacks
import java.util.logging.Logger

@Suppress("unused")
val noAdsPatch = bytecodePatch(
    name = "No Ads",
    description = "Blocks all non-rewarded ads: interstitial, banner, app open, MREC. " +
            "WARNING: Enabling Ads Free Rewards " +
            "alongside this patch will cause it to NOT work (No Ads takes priority).",
    default = false,
) {
    execute {
        // ── Bail out early if no supported SDK is present ──
        val hasMaxUnity = ShowInterstitialFingerprint.methodOrNull != null ||
            ShowAppOpenAdFingerprint.methodOrNull != null ||
            ShowBannerFingerprint.methodOrNull != null
        val hasNativeMax = MaxInterstitialAdShowAdFingerprint.methodOrNull != null ||
            MaxAppOpenAdShowAdFingerprint.methodOrNull != null ||
            MaxAdViewStartAutoRefreshFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax) {
            return@execute Logger.getLogger(this::class.java.name)
                .warning("Could not find supported ad SDK (MAX Unity or native MAX). No changes applied.")
        }

        // ── MAX Unity wrapper ──
        ShowInterstitialFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        ShowAppOpenAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        ShowBannerFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        ShowMRecFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        StartBannerAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        StartMRecAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }

        // ── Native MAX (non-Unity) ──
        // Reflection-based callback invocation to avoid crashes from NOP'ing showAd().
        // The game expects onAdDisplayed + onAdHidden callbacks to proceed.
        MaxInterstitialAdShowAdFingerprint.methodOrNull?.let {
            it.addInstructions(0, fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxInterstitialAd;"))
        }
        MaxAppOpenAdShowAdFingerprint.methodOrNull?.let {
            it.addInstructions(0, fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxAppOpenAd;"))
        }
        MaxAdViewStartAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }

        // ── Disable rewarded ads (conflict with Ads Free Rewards) ──
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
