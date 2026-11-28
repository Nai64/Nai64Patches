package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import patches.universal.ads.util.cloneParameters
import patches.universal.ads.util.findMutableMethodOf
import patches.universal.ads.util.fireRewardedAdCallbacks
import patches.universal.ads.util.repeatLines
import java.util.logging.Logger
import patches.universal.ads.util.DiscordPromo

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    description = "Get rewards without watching ads.",
    default = false,
) {
    val rewardMultiplier by stringOption(
        key = "rewardMultiplier",
        default = "1",
        title = "Reward multiplier",
        description = "How many times the reward callback runs per ad.",
    )
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        DiscordPromo.logOnce(logger)
        // Claiming the reward without showing the ad, reporting ads as
        // available, and dropping the countdown are what this patch is for.
        // They used to be switches that only ever made the patch do nothing,
        // so they are unconditional now.
        val instantReward: Boolean? = true
        val fakeAdAvailability: Boolean? = true
        val skipAdCountdown: Boolean? = true
        val requested = (rewardMultiplier ?: "1").trim()
        val parsed = requested.toIntOrNull()
        val multiplier = when {
            parsed == null -> {
                logger.warning("Ads Free Rewards: rewardMultiplier '$requested' is not a number - using 1")
                1
            }
            parsed < 1 -> {
                logger.warning("Ads Free Rewards: rewardMultiplier $parsed is below 1 - using 1")
                1
            }
            parsed > MAX_REPEAT_MULTIPLIER -> {
                logger.warning("Ads Free Rewards: rewardMultiplier $parsed is above the $MAX_REPEAT_MULTIPLIER limit - using $MAX_REPEAT_MULTIPLIER")
                MAX_REPEAT_MULTIPLIER
            }
            else -> parsed
        }
        logger.info("Ads Free Rewards: rewardMultiplier=$multiplier mode=reward")

        if (multiplier > 1) {
            logger.info("Ads Free Rewards: repeating the reward callback $multiplier time(s)")
        }
        val killed = skipAdCountdowns(logger)
        logger.info("Ads Free Rewards: removed ad countdown in $killed place(s)")
        applyAdsFreeRewardsV1190(logger, instantReward, multiplier)
        val faked = forceAdAvailability(logger)
        if (faked > 0) logger.info("Ads Free Rewards: faked availability for $faked SDK check(s)")
    }
}

/**
 * Ceiling for the reward multiplier.
 *
 * Repeating the callback writes the reward out once per step, so this is
 * bounded by dex output size rather than by int range. Each repeated
 * invoke-interface is 3 code units and a method body is capped at 65535, which
 * puts the theoretical ceiling near 21k. [repeatLines] caps the unroll at the
 * same value. Reaching much beyond this needs the repeat emitted as a counted
 * loop instead of an unroll, which needs a spare register at every injection
 * site; three of them do not currently grow their frames.
 */
private const val MAX_REPEAT_MULTIPLIER = 10_000

/**
 * Removes the rewarded-ad countdown.
 *
 * Rewarded units show a countdown and keep the reward control disabled until
 * it reaches zero, so the player waits out dead time. Folding the call that
 * shows the timer to a no-op removes both the countdown and the wait it
 * gates.
 */
private fun BytecodePatchContext.skipAdCountdowns(logger: Logger): Int {
    val targets = listOf(
        "Pangle TopLayoutDislike2.showCountDownText" to PangleCountdownShowFingerprint,
        "Pangle top/zmn.showCountDownText" to PangleCountdownShowAltFingerprint,
        "Pangle top/zn.showCountDownText" to PangleCountdownShowAlt2Fingerprint,
    )
    var patched = 0
    for ((label, fingerprint) in targets) {
        val method = fingerprint.methodOrNull
        if (method == null) {
            logger.info("Ads Free Rewards: countdown skipped $label - not present")
            continue
        }
        if (method.implementation == null) {
            logger.info("Ads Free Rewards: countdown skipped $label - no implementation")
            continue
        }
        try {
            method.addInstructions(0, "return-void")
            logger.info("Ads Free Rewards: removed countdown $label")
            patched++
        } catch (e: Exception) {
            logger.warning("Ads Free Rewards: countdown failed $label - ${e.message}")
        }
    }
    return patched
}

/**
 * Forces ad SDKs to report that an ad is currently available.
 *
 * Many games gate their rewarded / interstitial buttons behind an SDK
 * "isReady" / "isAvailable" check (e.g. Unity Ads Advertisement.isReady,
 * ironSource isRewardedVideoAvailable, AppLovin MAX isReady, or the Yandex
 * MyTarget mediation adapter's isLoaded). When no real ad can fill (no
 * network, ad-blocker, or re-signed build), these return false and the game
 * never calls show(), so the instant-reward hooks above never fire. Forcing
 * the gates to return true makes the game proceed to show(), letting the
 * reward flow grant without a real ad.
 */
private fun BytecodePatchContext.forceAdAvailability(logger: Logger): Int {
    var patched = 0
    fun patchIsReady(label: String, fingerprint: app.morphe.patcher.Fingerprint) {
        val method = fingerprint.methodOrNull ?: return
        val impl = method.implementation ?: run {
            logger.warning("Ads Free Rewards: skip $label  -  no implementation")
            return
        }
        if (impl.registerCount < 1) {
            logger.warning("Ads Free Rewards: skip $label  -  registerCount ${impl.registerCount} < 1")
            return
        }
        method.addInstructions(
            0,
            """
            const/4 v0, 0x1
            return v0
            """.trimIndent(),
        )
        logger.info("Ads Free Rewards: faked availability for $label")
        patched++
    }

    patchIsReady("Unity Ads Advertisement.isReady()", UnityAdsAdvertisementIsReadyFingerprint)
    patchIsReady("Unity Ads Advertisement.isReady(placement)", UnityAdsAdvertisementIsReadyPlacementFingerprint)
    patchIsReady("Unity Ads UnityAds.isReady()", UnityAdsSdkIsReadyFingerprint)
    patchIsReady("ironSource isRewardedVideoAvailable()", IronSourceIsRewardedVideoAvailableFingerprint)
    patchIsReady("ironSource isInterstitialReady()", IronSourceIsInterstitialReadyFingerprint)
    patchIsReady("AppLovin MAX InterstitialAd.isReady()", MaxInterstitialAdIsReadyFingerprint)
    patchIsReady("AppLovin MAX AppOpenAd.isReady()", MaxAppOpenAdIsReadyFingerprint)
    patchIsReady("Yandex/MyTarget rewarded mediation isLoaded()", YandexMyTargetRewardedIsLoadedFingerprint)
    patchIsReady("Yandex/MyTarget interstitial mediation isLoaded()", YandexMyTargetInterstitialIsLoadedFingerprint)
    patchIsReady("Huawei Ads Kit RewardAd.isLoaded()", HuaweiRewardAdIsLoadedFingerprint)
    patchIsReady("InMobi isReady()", InMobiIsReadyFingerprint)
    patchIsReady("AdMob rewarded isLoaded()", AdMobRewardedShowFingerprint) // will fail gracefully, just to log
    return patched
}

private fun BytecodePatchContext.applyAdsFreeRewardsV1190(logger: Logger, instantReward: Boolean?, multiplier: Int) {
    val useMax = true
    val useUnityAds = true
    val useIronSource = true
    val useRustore = true
    val useHuawei = true

    logger.info("Ads Free Rewards: instantReward=$instantReward fakeAdAvailability=true rewardMultiplier=$multiplier")

    val hasMaxUnity = ShowRewardedAdFingerprint.methodOrNull != null &&
        IsRewardedAdReadyFingerprint.methodOrNull != null
    val hasNativeMax = MaxRewardedAdIsReadyFingerprint.methodOrNull != null &&
        MaxRewardedAdShowAdFingerprint.methodOrNull != null
    val hasUnityAds = UnityRewardedAdShowFingerprint.methodOrNull != null
    val hasUnityAdsV4 = UnityAdsV4Show3ArgFingerprint.methodOrNull != null ||
        UnityAdsV4Show4ArgFingerprint.methodOrNull != null
    val hasLevelPlay = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull != null
    val hasIronSourceUnityBridge = IronSourceUnityRewardedAdIsReadyFingerprint.methodOrNull != null &&
        IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull != null
    val hasMyTarget = MyTargetBaseInterstitialShowFingerprint.methodOrNull != null
    val hasYandexUnityRewarded = YandexUnityRewardedWrapperShowFingerprint.methodOrNull != null
    val hasHuawei = HuaweiRewardAdIsLoadedFingerprint.methodOrNull != null &&
        HuaweiRewardAdShowFingerprint.methodOrNull != null
    val hasAdMob = AdMobRewardedShowFingerprint.methodOrNull != null
    val hasInMobi = InMobiInterstitialShowFingerprint.methodOrNull != null || InMobiRewardedShowFingerprint.methodOrNull != null
    val hasInMobiRewarded = InMobiRewardedShowFingerprint.methodOrNull != null
    val hasIronSourceAds = IronSourceAdsRewardedShowFingerprint.methodOrNull != null ||
        IronSourceAdsRewardedShowPreciseFingerprint.methodOrNull != null
    val hasMads = MadsWrapperShowAdFingerprint.methodOrNull != null &&
        MadsRvHandlerOnRewardedFingerprint.methodOrNull != null

    if (multiplier != 1) {
        logger.info("Ads Free Rewards: reward multiplier $multiplier - the ad break is repeated $multiplier time(s)")
    }

    logger.info("Ads Free Rewards: detected SDKs  -  MAX Unity=$hasMaxUnity native MAX=$hasNativeMax UnityAds=$hasUnityAds UnityAdsV4=$hasUnityAdsV4 LevelPlay=$hasLevelPlay ironSourceBridge=$hasIronSourceUnityBridge MyTarget=$hasMyTarget Yandex=$hasYandexUnityRewarded Huawei=$hasHuawei AdMob=$hasAdMob InMobi=$hasInMobi InMobiRewarded=$hasInMobiRewarded IronSourceAds=$hasIronSourceAds MADS=$hasMads")

    if (!hasMaxUnity && !hasNativeMax && !hasUnityAds && !hasUnityAdsV4 && !hasLevelPlay && !hasIronSourceUnityBridge && !hasMyTarget && !hasYandexUnityRewarded && !hasHuawei && !hasAdMob && !hasInMobiRewarded && !hasIronSourceAds && !hasMads) {
        logger.warning("Ads Free Rewards: no supported ad SDK found, no changes applied")
        return
    }

    // -- Huawei Ads Kit / Petal Ads --
    // Huawei's rewarded callback carries the reward object as a singleton
    // DEFAULT value, so no SDK-internal implementation class is required.
    val huaweiReady = HuaweiRewardAdIsLoadedFingerprint.methodOrNull
    val huaweiShow = HuaweiRewardAdShowFingerprint.methodOrNull
    if (useHuawei && instantReward == true && huaweiReady != null && huaweiShow != null) {
        val showClass = HuaweiRewardAdShowFingerprint.classDefOrNull
        if (showClass != null) {
            val huaweiOnRewarded = "invoke-virtual {v1, v0}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewarded(Lcom/huawei/hms/ads/reward/Reward;)V"
            huaweiShow.cloneParameters().addInstructions(
                0, """
                move-object/from16 v1, p2
                if-eqz v1, :morphe_huawei_reward_done
                invoke-virtual {v1}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewardAdOpened()V
                sget-object v0, Lcom/huawei/hms/ads/reward/Reward;->DEFAULT:Lcom/huawei/hms/ads/reward/Reward;
                ${repeatLines(multiplier, "                ", huaweiOnRewarded)}
                invoke-virtual {v1}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewardAdClosed()V
                :morphe_huawei_reward_done
                return-void
            """.trimIndent()
            )
            logger.info("Huawei Ads Kit rewarded patch succeeded${if (multiplier > 1) " (reward x$multiplier)" else ""}")
        } else {
            logger.warning("Ads Free Rewards: Huawei show class not found  -  skipping")
        }
    } else {
        if (!hasHuawei) logger.info("Ads Free Rewards: Huawei SDK not detected  -  skipping")
        else logger.info("Ads Free Rewards: Huawei skipped (instantReward=$instantReward)")
    }

    if (useRustore && instantReward == true) {
        applyMyTargetStrategy(logger, multiplier)
        applyYandexWrapperStrategy(logger, multiplier)
    }
    if (applyMaxUnityStrategy(logger, useMax, instantReward, multiplier)) return
    applyNativeMaxStrategy(logger, useMax, instantReward, multiplier)
    applyInMobiRewardedStrategy(logger, useMax, instantReward, multiplier)
    applyIronSourceAdsStrategy(logger, useIronSource, instantReward, multiplier)
    applyIronSourceAdsWrapperStrategy(logger, useIronSource, instantReward)
    applyMadsStrategy(logger, useIronSource, instantReward, multiplier)
    applyAdMobRewardedStrategy(logger, useMax, instantReward, multiplier)
    applyAdMobRewardRepeatStrategy(logger, multiplier)
    applyLevelPlayStrategy(logger, useIronSource)
    if (applyIronSourceBridgeStrategy(logger, useIronSource, instantReward, multiplier)) return
    applyUnityAdsStrategy(logger, useUnityAds, instantReward, multiplier)
    applyUnityAdsV4Strategy(logger, useUnityAds, instantReward, multiplier)
}

private fun BytecodePatchContext.applyMyTargetStrategy(logger: Logger, multiplier: Int) {
    val myTargetShow = MyTargetBaseInterstitialShowFingerprint.methodOrNull ?: return
    val hasShow = myTargetShow.implementation?.registerCount ?: 0 >= 2
    if (!hasShow) {
        logger.warning("Ads Free Rewards: skip MyTarget  -  low registerCount")
        return
    }
    val myTargetOnReward = "invoke-interface {v0, v2, v1}, Lcom/my/target/ads/RewardedAd\$RewardedAdListener;->onReward(Lcom/my/target/ads/Reward;Lcom/my/target/ads/RewardedAd;)V"
    myTargetShow.cloneParameters().addInstructions(
        0, """
        move-object/from16 v1, p0
        instance-of v0, v1, Lcom/my/target/ads/RewardedAd;
        if-eqz v0, :morphe_rustore_mytarget_original_show
        check-cast v1, Lcom/my/target/ads/RewardedAd;
        invoke-virtual {v1}, Lcom/my/target/ads/RewardedAd;->getListener()Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;
        move-result-object v0
        if-eqz v0, :morphe_rustore_mytarget_done
        invoke-interface {v0, v1}, Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;->onDisplay(Lcom/my/target/ads/RewardedAd;)V
        invoke-static {}, Lcom/my/target/ads/Reward;->getDefault()Lcom/my/target/ads/Reward;
        move-result-object v2
        ${repeatLines(multiplier, "        ", myTargetOnReward)}
        invoke-interface {v0, v1}, Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;->onDismiss(Lcom/my/target/ads/RewardedAd;)V
        :morphe_rustore_mytarget_done
        return-void
        :morphe_rustore_mytarget_original_show
    """.trimIndent()
    )
    logger.info("Ads Free Rewards: RuStore / VK MyTarget rewarded patch succeeded${if (multiplier > 1) " (reward x$multiplier)" else ""}")
}

private fun BytecodePatchContext.applyYandexWrapperStrategy(logger: Logger, multiplier: Int) {
    val yandexRewardedShow = YandexUnityRewardedWrapperShowFingerprint.methodOrNull ?: return
    val yandexOnRewarded = YandexUnityRewardedListenerOnRewardedFingerprint.methodOrNull ?: return
    val yandexOnRewardedCall = "invoke-interface {v0, v1, v2}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;->onRewarded(ILjava/lang/String;)V"
    yandexOnRewarded.cloneParameters().addInstructions(0, """
        move-object/from16 v2, p0
        iget-object v0, v2, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;
        if-eqz v0, :morphe_rustore_yandex_reward_done
        const/4 v1, 0x1
        const-string v2, "default"
        ${repeatLines(multiplier, "        ", yandexOnRewardedCall)}
        :morphe_rustore_yandex_reward_done
        return-void
    """.trimIndent())
    val clonedShow = yandexRewardedShow.cloneParameters()
    // Replace the original method instead of prepending to it. Yandex's wrapper
    // can contain branch targets that become invalid when instructions are
    // inserted before its existing implementation, causing VerifyError at runtime.
    val showImplementation = clonedShow.implementation ?: return
    showImplementation.removeInstructions(showImplementation.instructions.size)
    clonedShow.addInstructions(0, """
        move-object/from16 v1, p0
        iget-object v0, v1, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/RewardedAdWrapper;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;
        if-eqz v0, :morphe_rustore_yandex_show_done
        invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdShown()V
        const/4 v1, 0x0
        invoke-virtual {v0, v1}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onRewarded(Lcom/yandex/mobile/ads/rewarded/Reward;)V
        invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdDismissed()V
        :morphe_rustore_yandex_show_done
        return-void
    """.trimIndent())
    logger.info("Ads Free Rewards: RuStore / Yandex Unity rewarded patch succeeded${if (multiplier > 1) " (reward x$multiplier)" else ""}")
}

private fun BytecodePatchContext.applyInMobiRewardedStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?, multiplier: Int) {
    if (instantReward != true) return
    if (multiplier > 1) logger.warning("Ads Free Rewards: InMobi show is forced to success - there is no reward callback to repeat for this network")
    val inMobiShow = InMobiInterstitialShowFingerprint.methodOrNull ?: InMobiRewardedShowFingerprint.methodOrNull ?: return
    // InMobi mediated via AppLovin MAX - patching the show to instantly reward covers both interstitial and rewarded
    // Use the rewarded fingerprint if available, otherwise fallback to interstitial
    val target = InMobiRewardedShowFingerprint.methodOrNull ?: InMobiInterstitialShowFingerprint.methodOrNull ?: return
    try {
        target.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
        logger.info("Ads Free Rewards: InMobi patch - forced show to success")
    } catch (e: Exception) {
        logger.warning("Ads Free Rewards: InMobi patch failed: ${e.message}")
    }
}

private fun BytecodePatchContext.applyIronSourceAdsStrategy(logger: Logger, useIronSource: Boolean, instantReward: Boolean?, multiplier: Int) {
    if (instantReward != true || !useIronSource) return
    if (multiplier > 1) logger.warning("Ads Free Rewards: ironSourceAds show is forced to success - there is no reward callback to repeat for this network")
    val ironAds = IronSourceAdsRewardedShowFingerprint.methodOrNull ?: return
    try {
        ironAds.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
        logger.info("Ads Free Rewards: Unity IronSourceAds patch - forced show to success")
    } catch (e: Exception) {
        logger.warning("Ads Free Rewards: IronSourceAds patch failed: ${e.message}")
    }
}

// Precise ironSourceAds Unity wrapper (com.unity3d.ironsourceads.rewarded.RewardedAd).
// Fires shown -> earned -> dismissed on the registered listener so the Unity
// side grants the reward without a real ad. isReadyToShow is RV-only, force true.
private fun BytecodePatchContext.applyIronSourceAdsWrapperStrategy(logger: Logger, useIronSource: Boolean, instantReward: Boolean?) {
    if (instantReward != true || !useIronSource) return
    val ready = IronSourceAdsRewardedIsReadyPreciseFingerprint.methodOrNull
    val show = IronSourceAdsRewardedShowPreciseFingerprint.methodOrNull
    if (ready == null || show == null) return
    try {
        ready.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
        show.cloneParameters().addInstructions(
            0, """
            move-object/from16 v1, p0
            invoke-virtual {v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAd;->getListener()Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;
            move-result-object v0
            if-eqz v0, :morphe_isads_done
            invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdShown(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
            invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onUserEarnedReward(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
            invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdDismissed(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
            :morphe_isads_done
            return-void
        """.trimIndent()
        )
        logger.info("Ads Free Rewards: ironSourceAds wrapper patch succeeded (instant reward)")
    } catch (e: Exception) {
        logger.warning("Ads Free Rewards: ironSourceAds wrapper patch failed: ${e.message}")
    }
}

// Miniclip MADS mediation (madsunityplugin). The Unity-facing entry is
// MAdsAdsManagementWrapper.showAd(format, entryPoint, params) which serves
// banners, interstitials AND rewarded videos, so the injection checks the
// format against the RewardedVideos enum id and falls through to the
// original body for other formats. Reward fires via the RV handler
// singleton with a fresh info + reward payload.
private fun BytecodePatchContext.applyMadsStrategy(logger: Logger, useIronSource: Boolean, instantReward: Boolean?, multiplier: Int) {
    if (instantReward != true || !useIronSource) return
    val show = MadsWrapperShowAdFingerprint.methodOrNull ?: return
    if (MadsRvHandlerOnRewardedFingerprint.methodOrNull == null) return
    val enumClass = try {
        mutableClassDefByOrNull("Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils\$MAdsWrapperAdFormat;")
    } catch (_: Exception) {
        null
    }
    val hasRvEnum = enumClass?.fields?.any { it.name == "RewardedVideos" } == true &&
        enumClass.fields.any { it.name == "id" }
    if (!hasRvEnum) {
        logger.warning("Ads Free Rewards: MADS patch skipped  -  RewardedVideos enum not found")
        return
    }
    try {
        val madsReward = """
            sget-object v0, Lcom/miniclip/madsandroidsdk/base/adunit/RewardedVideosAdHandler;->INSTANCE:Lcom/miniclip/madsandroidsdk/base/adunit/RewardedVideosAdHandler;
            new-instance v1, Lcom/miniclip/madsandroidsdk/base/MediationAdInfo;
            invoke-direct {v1}, Lcom/miniclip/madsandroidsdk/base/MediationAdInfo;-><init>()V
            new-instance v2, Lcom/miniclip/madsandroidsdk/base/Reward;
            const-string v3, "1.0"
            invoke-static {v3}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D
            move-result-wide v6
            const-string v3, "reward"
            invoke-direct {v2, v3, v6, v7}, Lcom/miniclip/madsandroidsdk/base/Reward;-><init>(Ljava/lang/String;D)V
            move-object/from16 v3, p2
            invoke-virtual {v0, v1, v2, v3}, Lcom/miniclip/madsandroidsdk/base/adunit/RewardedVideosAdHandler;->onAdRewarded(Lcom/miniclip/madsandroidsdk/base/MediationAdInfo;Lcom/miniclip/madsandroidsdk/base/Reward;Ljava/lang/String;)V
        """.trimIndent()
        show.cloneParameters().addInstructions(
            0, """
            sget-object v0, Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;->RewardedVideos:Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;
            iget v0, v0, Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;->id:I
            move/from16 v4, p1
            if-ne v4, v0, :morphe_mads_original
            ${repeatLines(multiplier, "            ", madsReward)}
            const/4 v0, 0x1
            return v0
            :morphe_mads_original
        """.trimIndent()
        )
        logger.info("Ads Free Rewards: MADS patch succeeded (instant reward${if (multiplier > 1) ", reward x$multiplier" else ""})")
        val ready = MadsWrapperIsReadyFingerprint.methodOrNull
        if (ready != null) {
            ready.cloneParameters().addInstructions(
                0, """
                sget-object v0, Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;->RewardedVideos:Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;
                iget v0, v0, Lcom/miniclip/madsunityplugin/utils/MAdsSDKWrapperUtils${'$'}MAdsWrapperAdFormat;->id:I
                move/from16 v1, p1
                if-ne v1, v0, :morphe_mads_ready_original
                const/4 v0, 0x1
                return v0
                :morphe_mads_ready_original
            """.trimIndent()
            )
            logger.info("Ads Free Rewards: MADS patch succeeded (fake ready)")
        }
    } catch (e: Exception) {
        logger.warning("Ads Free Rewards: MADS patch failed: ${e.message}")
    }
}

private fun BytecodePatchContext.applyMaxUnityStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?, multiplier: Int): Boolean {
    val unityShow = ShowRewardedAdFingerprint.methodOrNull
    val unityReady = IsRewardedAdReadyFingerprint.methodOrNull
    if (!useMax || unityShow == null || unityReady == null) return false
    logger.info("Ads Free Rewards: MAX Unity Ad wrapper patch succeeded${if (instantReward == true && multiplier > 1) " (reward x$multiplier)" else ""}")
    unityReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        val unityRewardEvent = """
            new-instance v1, Lorg/json/JSONObject;
            invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
            const-string v2, "name"
            const-string v3, "OnRewardedAdReceivedRewardEvent"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adUnitId"
            invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adFormat"
            const-string v3, "rewarded"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "rewardLabel"
            const-string v3, "reward"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "rewardAmount"
            const-string v3, "1"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
        """.trimIndent()
        val unityDisplayedEvent = """
            new-instance v1, Lorg/json/JSONObject;
            invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
            const-string v2, "name"
            const-string v3, "OnRewardedAdDisplayedEvent"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adUnitId"
            invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adFormat"
            const-string v3, "rewarded"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
        """.trimIndent()
        val unityHiddenEvent = """
            new-instance v1, Lorg/json/JSONObject;
            invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
            const-string v2, "name"
            const-string v3, "OnRewardedAdHiddenEvent"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adUnitId"
            invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string v2, "adFormat"
            const-string v3, "rewarded"
            invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
        """.trimIndent()
        // The whole ad break is repeated, not just the reward callback. Games that
        // collapse repeated reward callbacks within a single ad break - common
        // in Unity IL2CPP titles, where one break means one payout no matter how
        // many reward events arrive - would otherwise pay out exactly once.
        // Replaying displayed -> reward -> hidden gives every iteration its own
        // break, so the guard resets in between and each one pays out.
        val oneBreak = "$unityDisplayedEvent\n$unityRewardEvent\n$unityHiddenEvent"
        val body = repeatLines(multiplier, "            ", oneBreak)
        unityShow.cloneParameters().addInstructions(
            0, """
            move-object/from16 v0, p1
            $body
            return-void
        """.trimIndent()
        )
        val unityLoad = LoadRewardedAdFingerprint.methodOrNull
        if (unityLoad != null) {
            logger.info("Ads Free Rewards: MAX Unity loadRewardedAd patching")
            unityLoad.cloneParameters().addInstructions(
                0, """
                move-object/from16 v0, p1
                new-instance v1, Lorg/json/JSONObject;
                invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
                const-string v2, "name"
                const-string v3, "OnRewardedAdLoadedEvent"
                invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v2, "adUnitId"
                invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v2, "adFormat"
                const-string v3, "rewarded"
                invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                return-void
            """.trimIndent()
            )
        }
    }
    return true
}

private fun BytecodePatchContext.applyNativeMaxStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?, multiplier: Int) {
    val nativeReady = MaxRewardedAdIsReadyFingerprint.methodOrNull
    val nativeShow = MaxRewardedAdShowAdFingerprint.methodOrNull
    if (!useMax || nativeReady == null || nativeShow == null) return
    logger.info("Ads Free Rewards: native MAX patch succeeded${if (instantReward == true && multiplier > 1) " (reward x$multiplier)" else ""}")
    nativeReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        val rc = nativeShow.implementation?.registerCount ?: 0
        if (rc >= 7) {
            nativeShow.addInstructions(0, fireRewardedAdCallbacks(multiplier))
        } else {
            try {
                nativeShow.cloneParameters()
                    .addInstructions(0, fireRewardedAdCallbacks(multiplier))
                logger.info("Ads Free Rewards: native MAX via clone (low regs $rc)")
            } catch (e: Exception) {
                logger.warning("Ads Free Rewards: clone failed for native MAX: ${e.message}")
            }
        }
    }
}

private fun BytecodePatchContext.applyAdMobRewardedStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?, multiplier: Int) {
    if (!useMax || instantReward != true) return
    // AdMob RewardedAd is from GMS (not in app dex), so scan call sites instead of definitions
    var patchedCallSites = 0
    val patchedClasses = mutableListOf<String>()
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("okhttp") || tl.contains("androidx") || tl.contains("com/google/android/gms/ads/rewarded")) return@classDefForEach
        // Must create mutable classes/methods only if required to prevent out of memory errors.
        val mutableClass by lazy { mutableClassDefBy(classDef) }
        for (method in classDef.methods) {
            val mutableMethod by lazy {
                mutableClass.findMutableMethodOf(method)
            }
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for (insn in instructions) {
                val ref = (insn as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference ?: continue
                if (ref.definingClass != "Lcom/google/android/gms/ads/rewarded/RewardedAd;" || ref.name != "show" || ref.returnType != "V") continue
                if (ref.parameterTypes.size != 2 || ref.parameterTypes[1] != "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;") continue
                // Record the wrapper but do NOT synthesise a reward here. Firing a fabricated
                // onUserEarnedReward(null) at show() time both corrupts the method (removeInstruction
                // re-indexes and ART rejects the injected branch) and crashes wrappers that dereference
                // the RewardItem. Multiplication happens in applyAdMobRewardRepeatStrategy instead.
                if (classDef.type !in patchedClasses) patchedClasses.add(classDef.type)
                patchedCallSites++
            }
        }
    }
    // Cordova/web wrappers gate show() behind their own readiness probe (isLoaded() -> mAd != null)
    // and refuse to render otherwise. Force those probes true in the wrappers we found, otherwise
    // the ad button never reaches the show path when no ad is currently served.
    var fakedReady = 0
    if (patchedCallSites > 0) {
        classDefForEach { classDef ->
            if (classDef.type !in patchedClasses) return@classDefForEach
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                if (method.name != "isLoaded" && method.name != "isReady") continue
                if (method.parameterTypes.isNotEmpty() || method.returnType != "Z") continue
                if (com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(method.accessFlags)) continue
                try {
                    mutableClass.findMutableMethodOf(method).addInstructions(0, """
                        const/4 v0, 0x1
                        return v0
                    """.trimIndent())
                    fakedReady++
                } catch (_: Exception) {}
            }
        }
    }
    if (patchedCallSites > 0) {
        logger.info("Ads Free Rewards: AdMob detected $patchedCallSites show call site(s), faked $fakedReady readiness check(s)")
    } else {
        logger.info("Ads Free Rewards: AdMob show call sites not found - skipping")
    }
}

// Universal multiplier for AdMob-direct wrappers.
//
// The AdMob SDK fires OnUserEarnedRewardListener.onUserEarnedReward(RewardItem) exactly once per
// watched ad, so the reward can only be multiplied at the receiver: repeat whatever the game does
// with the reward inside its own listener implementation. That keeps a *real* RewardItem in play,
// which matters because wrappers typically dereference it (admob-plus Cordova's emit() reads
// getAmount()/getType(), so a fabricated null RewardItem crashes the game).
//
// Firing a synthetic callback at show() time cannot work and is deliberately not attempted: it
// both crashes wrappers and skips the real ad, so the player never earns anything legitimately.
private val REWARD_SKIP_OWNERS = listOf(
    "analytics", "appmetrica", "databuckets", "adjust", "firebase", "flurry",
    "http", "okhttp", "retrofit", "socket", "applovin", "facebook", "appsflyer",
)

private fun BytecodePatchContext.applyAdMobRewardRepeatStrategy(logger: Logger, multiplier: Int) {
    if (multiplier <= 1) return
    var patchedHandlers = 0
    var repeatedInvokes = 0
    classDefForEach { classDef ->
        for (method in classDef.methods) {
if (method.name != "onUserEarnedReward") continue
                if (method.parameterTypes.size != 1) continue
                if (method.parameterTypes[0] != "Lcom/google/android/gms/ads/rewarded/RewardItem;") continue
                // Never rewrite the mediation SDK's own internals: duplicating its callbacks
                // corrupts mediation state rather than granting the player anything.
                if (classDef.type.startsWith("Lcom/google/android/gms/")) continue
                val impl = method.implementation ?: continue
            if (impl.instructions.none { true }) continue
            // Parameters are allocated at the top of the frame, so the single RewardItem argument
            // is always the highest register. Deriving it as registerCount - (params + this) would
            // point at `this` for interface implementations, where the frame holds a leading local.
            val rewardReg = impl.registerCount - 1
            if (rewardReg < 0) continue
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
            val instructions = impl.instructions.toList()
            // Collected first: inserting shifts the indices of everything after it.
            val repeats = mutableListOf<Pair<Int, String>>()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference ?: continue
                val owner = ref.definingClass.lowercase()
                if (REWARD_SKIP_OWNERS.any { owner.contains(it) }) continue
                val registers = when (insn) {
                    is com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction ->
                        listOf(insn.registerC, insn.registerD, insn.registerE, insn.registerF, insn.registerG).take(insn.registerCount)
                    is com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction ->
                        (insn.startRegister until insn.startRegister + insn.registerCount).toList()
                    else -> continue
                }
                if (registers.size < 2 || registers.size > 5) continue
                // The reward must be handed onward as an argument, not be the receiver. Wrappers
                // either read it directly (getAmount/getType) or delegate to another handler.
                if (!registers.drop(1).contains(rewardReg)) continue
                val text = "${insn.opcode.name} {${registers.joinToString(", ") { "v$it" }}}, ${ref}"
                repeats.add(index to text)
            }
            if (repeats.isEmpty()) continue
            // Back to front so earlier indices stay valid while we insert.
            for ((index, text) in repeats.sortedByDescending { it.first }) {
                try {
                    val copies = (2..multiplier).joinToString("\n") { text }
                    mutableMethod.addInstructions(index + 1, copies)
                    repeatedInvokes++
                } catch (_: Exception) {}
            }
            patchedHandlers++
        }
    }
    if (patchedHandlers > 0) {
        logger.info("Ads Free Rewards: AdMob reward repeat - amplified $patchedHandlers listener handler(s), $repeatedInvokes call site(s) x$multiplier")
    } else {
        logger.info("Ads Free Rewards: AdMob reward repeat - no onUserEarnedReward handler found")
    }
}

private fun BytecodePatchContext.applyLevelPlayStrategy(logger: Logger, useIronSource: Boolean) {
    val levelPlayReady = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull ?: return
    if (!useIronSource) return
    levelPlayReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    logger.info("Ads Free Rewards: LevelPlay patch succeeded")
}

private fun BytecodePatchContext.applyIronSourceBridgeStrategy(logger: Logger, useIronSource: Boolean, instantReward: Boolean?, multiplier: Int): Boolean {
    val bridgeReady = IronSourceUnityRewardedAdIsReadyFingerprint.methodOrNull
    val bridgeShow = IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull
    if (!useIronSource || bridgeReady == null || bridgeShow == null) return false
    logger.info("Ads Free Rewards: IronSource patch succeeded${if (instantReward == true && multiplier > 1) " (reward x$multiplier)" else ""}")
    bridgeReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        val ironSourceReward = """
            new-instance v4, Lcom/unity3d/mediation/rewarded/LevelPlayReward;
            const-string v2, "reward"
            const/4 v3, 0x1
            invoke-direct {v4, v2, v3}, Lcom/unity3d/mediation/rewarded/LevelPlayReward;-><init>(Ljava/lang/String;I)V
            invoke-interface {v0, v4, v1}, Lcom/ironsource/Za;->onAdRewarded(Lcom/unity3d/mediation/rewarded/LevelPlayReward;Lcom/unity3d/mediation/LevelPlayAdInfo;)V
        """.trimIndent()
        bridgeShow.cloneParameters().addInstructions(0, """
            move-object/from16 v3, p0
            iget-object v0, v3, Lcom/ironsource/Ya;->k:Lcom/ironsource/Za;
            if-eqz v0, :morphe_ads_free_rewards_done
            iget-object v1, v3, Lcom/ironsource/Ya;->m:Lcom/ironsource/q6;
            invoke-interface {v1}, Lcom/ironsource/q6;->b()Lcom/unity3d/mediation/LevelPlayAdInfo;
            move-result-object v1
            invoke-interface {v0, v1}, Lcom/ironsource/Za;->onAdDisplayed(Lcom/unity3d/mediation/LevelPlayAdInfo;)V
            ${repeatLines(multiplier, "            ", ironSourceReward)}
            invoke-interface {v0, v1}, Lcom/ironsource/Za;->onAdClosed(Lcom/unity3d/mediation/LevelPlayAdInfo;)V
            :morphe_ads_free_rewards_done
            return-void
        """.trimIndent())
    }
    return true
}

private fun BytecodePatchContext.applyUnityAdsStrategy(logger: Logger, useUnityAds: Boolean, instantReward: Boolean?, multiplier: Int) {
    val adsShow = UnityRewardedAdShowFingerprint.methodOrNull ?: return
    if (!useUnityAds || instantReward != true) return
    val unityOnRewarded = "invoke-interface {v0, v1}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V"
    adsShow.addInstructions(0, """
        move-object/from16 v0, p3
        move-object/from16 v1, p0
        ${repeatLines(multiplier, "        ", unityOnRewarded)}
        invoke-interface {v0, v1}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
        sget-object v2, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
        invoke-interface {v0, v1, v2}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
        return-void
    """.trimIndent())
    logger.info("Ads Free Rewards: Unity Ads patch succeeded${if (multiplier > 1) " (reward x$multiplier)" else ""}")
}

private fun BytecodePatchContext.applyUnityAdsV4Strategy(logger: Logger, useUnityAds: Boolean, instantReward: Boolean?, multiplier: Int) {
    if (!useUnityAds || instantReward != true) return
    val unityAdsOnComplete = "invoke-interface {v1, v2, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V"
    val v4Show3 = UnityAdsV4Show3ArgFingerprint.methodOrNull
    if (v4Show3 != null) {
        v4Show3.addInstructions(0, """
            move-object/from16 v1, p2
            move-object/from16 v2, p1
            invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
            sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
            ${repeatLines(multiplier, "            ", unityAdsOnComplete)}
            return-void
        """.trimIndent())
        logger.info("Ads Free Rewards: Unity Ads v4 patch succeeded (3-arg show${if (multiplier > 1) ", reward x$multiplier" else ""})")
    }
    val v4Show4 = UnityAdsV4Show4ArgFingerprint.methodOrNull
    if (v4Show4 != null) {
        v4Show4.addInstructions(0, """
            move-object/from16 v1, p3
            move-object/from16 v2, p1
            invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
            sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
            ${repeatLines(multiplier, "            ", unityAdsOnComplete)}
            return-void
        """.trimIndent())
        logger.info("Ads Free Rewards: Unity Ads v4 patch succeeded (4-arg show${if (multiplier > 1) ", reward x$multiplier" else ""})")
    }
}

