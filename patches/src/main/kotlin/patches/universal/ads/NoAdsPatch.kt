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
        description = "Block full-screen ads shown between app content",
    )
    val blockBanners by booleanOption(
        title = "Block Banners",
        default = true,
        key = "blockBanners",
        description = "Block banner ads shown at the top or bottom of the screen",
    )
    val blockAppOpen by booleanOption(
        title = "Block App Open",
        default = true,
        key = "blockAppOpen",
        description = "Block ads shown when the app starts",
    )
    val blockMRec by booleanOption(
        title = "Block MREC",
        default = true,
        key = "blockMRec",
        description = "Block medium rectangle (MREC) banner ads",
    )
    val blockRewarded by booleanOption(
        title = "Block Rewarded",
        default = true,
        key = "blockRewarded",
        description = "Block rewarded ads; may disable features that require watching them",
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
            AdMobAppOpenLoadFingerprint.methodOrNull != null ||
            AdMobRewardedShowFingerprint.methodOrNull != null ||
            AdMobLegacyRewardedVideoShowFingerprint.methodOrNull != null
        val hasUnityAdsV3 = UnityAdsV3Show2ArgFingerprint.methodOrNull != null ||
            UnityAdsV3ShowOptionsFingerprint.methodOrNull != null
        val hasIronSource = IronSourceShowDemandOnlyInterstitialFingerprint.methodOrNull != null ||
            IronSourceShowDemandOnlyRewardedVideoFingerprint.methodOrNull != null ||
            IronSourceShowInterstitialFingerprint.methodOrNull != null ||
            IronSourceShowRewardedVideoFingerprint.methodOrNull != null
        val hasAppLovinLegacy = AppLovinInterstitialDialogShowFingerprint.methodOrNull != null ||
            AppLovinIncentivizedShow4ListenerFingerprint.methodOrNull != null ||
            AppLovinAdViewLoadNextAdFingerprint.methodOrNull != null
        val hasVungle = VungleBaseFullscreenAdLoadFingerprint.methodOrNull != null
        val hasFacebook = FacebookInterstitialAdShowFingerprint.methodOrNull != null ||
            FacebookRewardedVideoAdShowFingerprint.methodOrNull != null
        val hasPangle = PangleInterstitialShowFingerprint.methodOrNull != null ||
            PangleRewardedShowFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax && !hasAdMob && !hasUnityAdsV3 && !hasIronSource && !hasAppLovinLegacy && !hasVungle && !hasFacebook && !hasPangle) {
            logger.warning("Could not find supported ad SDK (MAX Unity, native MAX, AdMob, Unity Ads, ironSource, AppLovin, Vungle, Meta or Pangle). No changes applied.")
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
            AdMobAppOpenLoadFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
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

        // ── Unity Ads v3 (legacy) ──
        if (blockInterstitials == true) {
            UnityAdsV3Show2ArgFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            UnityAdsV3ShowOptionsFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── ironSource (LevelPlay) public API ──
        if (blockInterstitials == true) {
            IronSourceShowDemandOnlyInterstitialFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowInterstitialFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowInterstitialActivityFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowInterstitialActivityPlacementFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowInterstitialPlacementFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockRewarded == true) {
            IronSourceShowDemandOnlyRewardedVideoFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowRewardedVideoFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowRewardedVideoActivityFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowRewardedVideoActivityPlacementFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            IronSourceShowRewardedVideoPlacementFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── AppLovin legacy (direct SDK, non-MAX) ──
        if (blockInterstitials == true) {
            AppLovinInterstitialDialogShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AppLovinInterstitialDialogShowAndRenderFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockBanners == true) {
            AppLovinAdViewLoadNextAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockRewarded == true) {
            AppLovinIncentivizedShow4ListenerFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AppLovinIncentivizedShow5ListenerFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── Vungle ──
        if (blockInterstitials == true || blockRewarded == true) {
            VungleBaseFullscreenAdLoadFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        // ── Meta Audience Network (facebook/ads) ──
        if (blockInterstitials == true) {
            FacebookInterstitialAdShowFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            FacebookInterstitialAdShowConfigFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
        }
        if (blockRewarded == true) {
            FacebookRewardedVideoAdShowFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            FacebookRewardedVideoAdShowConfigFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            FacebookRewardedInterstitialShowFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            FacebookRewardedInterstitialShowConfigFingerprint.methodOrNull?.let {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
        }

        // ── Pangle (bytedance) ──
        if (blockInterstitials == true) {
            PangleInterstitialShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockAppOpen == true) {
            PangleAppOpenShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockRewarded == true) {
            PangleRewardedShowFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
    }
}
