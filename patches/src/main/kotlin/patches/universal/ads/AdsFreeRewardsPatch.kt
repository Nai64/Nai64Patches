package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.patch.BytecodePatchContext
import patches.universal.ads.util.cloneMutableAndPreserveParameters
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards (Experimental)",
    description = "Get rewards without watching ads. Combine with No Ads for other formats, but keep No Ads' rewarded block off.",
    default = false,
) {
    val patchVersion by stringOption(
        key = "patchVersion",
        default = "1.32.0",
        title = "Patch version",
        description = "Choose the implementation to use. Each version is a snapshot  -  newer ones support more networks. If the latest does not work for your app, try an older version.",
        values = linkedMapOf(
            "1.41.0" to "1.41.0",
            "1.40.0" to "1.40.0",
            "1.38.0" to "1.38.0",
            "1.34.0" to "1.34.0",
            "1.33.0" to "1.33.0",
            "1.32.0" to "1.32.0",
            "1.31.0" to "1.31.0",
            "1.30.0" to "1.30.0",
            "1.22.0" to "1.22.0",
            "1.21.0" to "1.21.0",
            "1.20.0" to "1.20.0",
            "1.19.0" to "1.19.0",
            "1.18.1" to "1.18.1",
            "1.18.0" to "1.18.0",
            "1.17.0" to "1.17.0",
            "1.16.0" to "1.16.0",
            "1.15.0" to "1.15.0",
            "1.1.0 (Original)" to "1.1.0",
        ),
    )
    val rewardStrategy by stringOption(
        key = "rewardStrategy",
        default = "auto",
        title = "Reward Strategy",
        description = "Which ad-network integration to patch. 'Auto' covers every supported network",
        values = mapOf(
            "Auto (all networks)" to "auto",
            "AppLovin MAX" to "max",
            "Unity Ads" to "unityAds",
            "ironSource / LevelPlay" to "ironSource",
            "RuStore / VK MyTarget" to "rustore",
            "Huawei Ads Kit / Petal Ads" to "huawei",
        ),
    )
    val instantReward by booleanOption(
        key = "instantReward",
        default = true,
        title = "Instant reward",
        description = "Claim the reward immediately without showing an ad (applies to the current patch version)",
    )
    val fakeAdAvailability by booleanOption(
        key = "fakeAdAvailability",
        default = true,
        title = "Fake ad availability",
        description = "Force ad SDKs to report ads as available so the reward flow triggers (needed when no real ad can fill)",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        when (patchVersion) {
            "1.1.0" -> applyAdsFreeRewardsV110(logger)
            "1.15.0" -> applyAdsFreeRewardsV1150(logger)
            "1.16.0" -> applyAdsFreeRewardsV1160(logger)
            "1.17.0" -> applyAdsFreeRewardsV1170(logger)
            "1.18.0" -> applyAdsFreeRewardsV1180(logger)
            "1.18.1" -> applyAdsFreeRewardsV1181(logger)
            "1.19.0" -> applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
            "1.20.0" -> applyAdsFreeRewardsV1200(logger, rewardStrategy, instantReward)
            "1.21.0" -> applyAdsFreeRewardsV1210(logger, rewardStrategy, instantReward)
            "1.22.0" -> applyAdsFreeRewardsV1220(logger, rewardStrategy, instantReward)
            "1.30.0" -> applyAdsFreeRewardsV1300(logger, rewardStrategy, instantReward)
            "1.31.0" -> applyAdsFreeRewardsV1310(logger, rewardStrategy, instantReward)
            "1.33.0" -> applyAdsFreeRewardsV1330(logger, rewardStrategy, instantReward)
            "1.34.0" -> applyAdsFreeRewardsV1340(logger, rewardStrategy, instantReward)
            "1.38.0" -> applyAdsFreeRewardsV1380(logger, rewardStrategy, instantReward)
            "1.40.0" -> applyAdsFreeRewardsV1400(logger, rewardStrategy, instantReward)
            "1.41.0" -> applyAdsFreeRewardsV1410(logger, rewardStrategy, instantReward)
            else -> applyAdsFreeRewardsV1320(logger, rewardStrategy, instantReward)
        }
        if (fakeAdAvailability == true) {
            val faked = forceAdAvailability(logger)
            if (faked > 0) logger.info("Ads Free Rewards: faked availability for $faked SDK check(s)")
        }
    }
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
    return patched
}

private fun BytecodePatchContext.applyAdsFreeRewardsV1190(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    val strategy = rewardStrategy
    val useMax = strategy == "auto" || strategy == "max"
    val useUnityAds = strategy == "auto" || strategy == "unityAds"
    val useIronSource = strategy == "auto" || strategy == "ironSource"
    val useRustore = strategy == "auto" || strategy == "rustore"
    val useHuawei = strategy == "auto" || strategy == "huawei"

    logger.info("Ads Free Rewards: strategy=$strategy instantReward=$instantReward fakeAdAvailability=${true} patchVersion=${"1.32.0"}")

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

    logger.info("Ads Free Rewards: detected SDKs  -  MAX Unity=$hasMaxUnity native MAX=$hasNativeMax UnityAds=$hasUnityAds UnityAdsV4=$hasUnityAdsV4 LevelPlay=$hasLevelPlay ironSourceBridge=$hasIronSourceUnityBridge MyTarget=$hasMyTarget Yandex=$hasYandexUnityRewarded Huawei=$hasHuawei AdMob=$hasAdMob")

    if (!hasMaxUnity && !hasNativeMax && !hasUnityAds && !hasUnityAdsV4 && !hasLevelPlay && !hasIronSourceUnityBridge && !hasMyTarget && !hasYandexUnityRewarded && !hasHuawei && !hasAdMob) {
        logger.warning("Ads Free Rewards: no supported ad SDK found for reward strategy $strategy  -  no changes applied")
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
            val clonedShow = huaweiShow.cloneMutableAndPreserveParameters(showClass)
            clonedShow.addInstructions(0, """
                if-eqz p2, :morphe_huawei_reward_done
                invoke-virtual {p2}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewardAdOpened()V
                sget-object v0, Lcom/huawei/hms/ads/reward/Reward;->DEFAULT:Lcom/huawei/hms/ads/reward/Reward;
                invoke-virtual {p2, v0}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewarded(Lcom/huawei/hms/ads/reward/Reward;)V
                invoke-virtual {p2}, Lcom/huawei/hms/ads/reward/RewardAdStatusListener;->onRewardAdClosed()V
                :morphe_huawei_reward_done
                return-void
            """.trimIndent())
            logger.info("Huawei Ads Kit rewarded patch succeeded")
        } else {
            logger.warning("Ads Free Rewards: Huawei show class not found  -  skipping")
        }
    } else {
        if (!useHuawei) logger.info("Ads Free Rewards: Huawei strategy disabled by rewardStrategy=$strategy")
        else if (!hasHuawei) logger.info("Ads Free Rewards: Huawei SDK not detected  -  skipping")
        else logger.info("Ads Free Rewards: Huawei skipped (instantReward=$instantReward)")
    }

    if (useRustore && instantReward == true) {
        applyMyTargetStrategy(logger)
        applyYandexWrapperStrategy(logger)
    }
    if (applyMaxUnityStrategy(logger, useMax, instantReward)) return
    applyNativeMaxStrategy(logger, useMax, instantReward)
    applyAdMobRewardedStrategy(logger, useMax, instantReward)
    applyLevelPlayStrategy(logger, useIronSource)
    if (applyIronSourceBridgeStrategy(logger, useIronSource, instantReward)) return
    applyUnityAdsStrategy(logger, useUnityAds, instantReward)
    applyUnityAdsV4Strategy(logger, useUnityAds, instantReward)
}

private fun BytecodePatchContext.applyMyTargetStrategy(logger: Logger) {
    val myTargetShow = MyTargetBaseInterstitialShowFingerprint.methodOrNull ?: return
    val hasShow = myTargetShow.implementation?.registerCount ?: 0 >= 2
    if (!hasShow) {
        logger.warning("Ads Free Rewards: skip MyTarget  -  low registerCount")
        return
    }
    val showClass = MyTargetBaseInterstitialShowFingerprint.classDefOrNull ?: return
    val cloned = myTargetShow.cloneMutableAndPreserveParameters(showClass)
    cloned.addInstructions(0, """
        instance-of v0, p0, Lcom/my/target/ads/RewardedAd;
        if-eqz v0, :morphe_rustore_mytarget_original_show
        check-cast p0, Lcom/my/target/ads/RewardedAd;
        invoke-virtual {p0}, Lcom/my/target/ads/RewardedAd;->getListener()Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;
        move-result-object v0
        if-eqz v0, :morphe_rustore_mytarget_done
        invoke-interface {v0, p0}, Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;->onDisplay(Lcom/my/target/ads/RewardedAd;)V
        invoke-static {}, Lcom/my/target/ads/Reward;->getDefault()Lcom/my/target/ads/Reward;
        move-result-object p1
        invoke-interface {v0, p1, p0}, Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;->onReward(Lcom/my/target/ads/Reward;Lcom/my/target/ads/RewardedAd;)V
        invoke-interface {v0, p0}, Lcom/my/target/ads/RewardedAd${'$'}RewardedAdListener;->onDismiss(Lcom/my/target/ads/RewardedAd;)V
        :morphe_rustore_mytarget_done
        return-void
        :morphe_rustore_mytarget_original_show
    """.trimIndent())
    logger.info("Ads Free Rewards: RuStore / VK MyTarget rewarded patch succeeded")
}

private fun BytecodePatchContext.applyYandexWrapperStrategy(logger: Logger) {
    val yandexRewardedShow = YandexUnityRewardedWrapperShowFingerprint.methodOrNull ?: return
    val yandexOnRewarded = YandexUnityRewardedListenerOnRewardedFingerprint.methodOrNull ?: return
    yandexOnRewarded.addInstructions(0, """
        iget-object v0, p0, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;
        if-eqz v0, :morphe_rustore_yandex_reward_done
        const/4 v1, 0x1
        const-string p1, "default"
        invoke-interface {v0, v1, p1}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;->onRewarded(ILjava/lang/String;)V
        :morphe_rustore_yandex_reward_done
        return-void
    """.trimIndent())
    val showClass = YandexUnityRewardedWrapperShowFingerprint.classDefOrNull ?: return
    val clonedShow = yandexRewardedShow.cloneMutableAndPreserveParameters(showClass)
    // Replace the original method instead of prepending to it. Yandex's wrapper
    // can contain branch targets that become invalid when instructions are
    // inserted before its existing implementation, causing VerifyError at runtime.
    val showImplementation = clonedShow.implementation ?: return
    showImplementation.removeInstructions(showImplementation.instructions.size)
    clonedShow.addInstructions(0, """
        iget-object v0, p0, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/RewardedAdWrapper;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;
        if-eqz v0, :morphe_rustore_yandex_show_done
        invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdShown()V
        const/4 v1, 0x0
        invoke-virtual {v0, v1}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onRewarded(Lcom/yandex/mobile/ads/rewarded/Reward;)V
        invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdDismissed()V
        :morphe_rustore_yandex_show_done
        return-void
    """.trimIndent())
    logger.info("Ads Free Rewards: RuStore / Yandex Unity rewarded patch succeeded")
}

private fun BytecodePatchContext.applyMaxUnityStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?): Boolean {
    val unityShow = ShowRewardedAdFingerprint.methodOrNull
    val unityReady = IsRewardedAdReadyFingerprint.methodOrNull
    if (!useMax || unityShow == null || unityReady == null) return false
    logger.info("Ads Free Rewards: MAX Unity Ad wrapper patch succeeded")
    unityReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        val showClass = ShowRewardedAdFingerprint.classDefOrNull ?: return true
        val clonedShow = unityShow.cloneMutableAndPreserveParameters(showClass)
        clonedShow.addInstructions(0, """
            move-object v0, p1
            new-instance p0, Lorg/json/JSONObject;
            invoke-direct {p0}, Lorg/json/JSONObject;-><init>()V
            const-string p1, "name"
            const-string p2, "OnRewardedAdDisplayedEvent"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adUnitId"
            invoke-static {p0, p1, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adFormat"
            const-string p2, "rewarded"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {p0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
            new-instance p0, Lorg/json/JSONObject;
            invoke-direct {p0}, Lorg/json/JSONObject;-><init>()V
            const-string p1, "name"
            const-string p2, "OnRewardedAdReceivedRewardEvent"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adUnitId"
            invoke-static {p0, p1, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adFormat"
            const-string p2, "rewarded"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "rewardLabel"
            const-string p2, "reward"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "rewardAmount"
            const-string p2, "1"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {p0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
            new-instance p0, Lorg/json/JSONObject;
            invoke-direct {p0}, Lorg/json/JSONObject;-><init>()V
            const-string p1, "name"
            const-string p2, "OnRewardedAdHiddenEvent"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adUnitId"
            invoke-static {p0, p1, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            const-string p1, "adFormat"
            const-string p2, "rewarded"
            invoke-static {p0, p1, p2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {p0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
            return-void
        """.trimIndent())
        val unityLoad = LoadRewardedAdFingerprint.methodOrNull
        if (unityLoad != null) {
            logger.info("Ads Free Rewards: MAX Unity loadRewardedAd patching")
            val loadClass = LoadRewardedAdFingerprint.classDefOrNull ?: return true
            val clonedLoad = unityLoad.cloneMutableAndPreserveParameters(loadClass)
            clonedLoad.addInstructions(0, """
                move-object v0, p1
                new-instance p0, Lorg/json/JSONObject;
                invoke-direct {p0}, Lorg/json/JSONObject;-><init>()V
                const-string p1, "name"
                const-string v1, "OnRewardedAdLoadedEvent"
                invoke-static {p0, p1, v1}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string p1, "adUnitId"
                invoke-static {p0, p1, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string p1, "adFormat"
                const-string v1, "rewarded"
                invoke-static {p0, p1, v1}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                invoke-static {p0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                return-void
            """.trimIndent())
        }
    }
    return true
}

private fun BytecodePatchContext.applyNativeMaxStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?) {
    val nativeReady = MaxRewardedAdIsReadyFingerprint.methodOrNull
    val nativeShow = MaxRewardedAdShowAdFingerprint.methodOrNull
    if (!useMax || nativeReady == null || nativeShow == null) return
    logger.info("Ads Free Rewards: native MAX patch succeeded")
    nativeReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        val rc = nativeShow.implementation?.registerCount ?: 0
        if (rc >= 7) {
            nativeShow.addInstructions(0, fireRewardedAdCallbacks())
        } else {
            val showClass = MaxRewardedAdShowAdFingerprint.classDefOrNull
            if (showClass != null) {
                try {
                    val cloned = nativeShow.cloneMutableAndPreserveParameters(showClass)
                    cloned.addInstructions(0, fireRewardedAdCallbacks())
                    logger.info("Ads Free Rewards: native MAX via clone (low regs $rc)")
                } catch (e: Exception) {
                    logger.warning("Ads Free Rewards: clone failed for native MAX: ${e.message}")
                }
            } else {
                logger.warning("Ads Free Rewards: skip native MAX showAd()  -  registerCount $rc < 7")
            }
        }
    }
}

private fun BytecodePatchContext.applyAdMobRewardedStrategy(logger: Logger, useMax: Boolean, instantReward: Boolean?) {
    if (!useMax || instantReward != true) return
    // AdMob RewardedAd is from GMS (not in app dex), so patch call sites instead of definition
    var patchedCallSites = 0
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("okhttp") || tl.contains("androidx") || tl.contains("com/google/android/gms/ads/rewarded")) return@classDefForEach
        val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference ?: continue
                if (ref.definingClass != "Lcom/google/android/gms/ads/rewarded/RewardedAd;" || ref.name != "show" || ref.returnType != "V") continue
                if (ref.parameterTypes.size != 2 || ref.parameterTypes[1] != "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;") continue
                // Found call site: RewardedAd.show(Activity, OnUserEarnedRewardListener)
                // Replace it with direct reward: if p2 != null, p2.onUserEarnedReward(null)
                try {
                    // Determine registers for Activity and listener (35c or 3rc)
                    val (activityReg, listenerReg) = when (insn) {
                        is com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c -> {
                            // invoke-virtual {v0, v1, v2}, RewardedAd.show
                            // v0 = this (RewardedAd), v1 = Activity, v2 = listener
                            // Need to parse: for 35c, registerCount, registers C/D/E etc.
                            // For show with 3 regs (this, activity, listener), C=this, D=activity, E=listener
                            if (insn.registerCount < 3) continue
                            Pair(insn.registerD, insn.registerE)
                        }
                        is com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc -> {
                            // invoke-virtual/range {v0..v2}
                            val start = insn.startRegister
                            Pair(start + 1, start + 2)
                        }
                        else -> continue
                    }
                    method.addInstructions(index, """
                        if-eqz v$listenerReg, :morphe_admob_skip_$index
                        const/4 v0, 0x0
                        invoke-interface {v$listenerReg, v0}, Lcom/google/android/gms/ads/OnUserEarnedRewardListener;->onUserEarnedReward(Lcom/google/android/gms/ads/rewarded/RewardItem;)V
                        :morphe_admob_skip_$index
                    """.trimIndent())
                    // Keep original invoke as well? Actually we want to skip the ad, so we should nop the original invoke
                    // Instead, we just inserted reward before, and let original show still run  -  it will show ad but also give reward instantly
                    // To fully skip ad, we could nop the invoke, but that may break flow; for now we give instant reward plus still show ad (user sees ad but also gets reward)
                    // For Ringdale, the ad is via MAX mediation, not direct, so this call site may not be the primary  -  but patching it still gives instant reward
                    patchedCallSites++
                } catch (_: Exception) {}
            }
        }
    }
    if (patchedCallSites > 0) {
        logger.info("Ads Free Rewards: AdMob patch - patched $patchedCallSites call site(s)")
    } else {
        logger.info("Ads Free Rewards: AdMob show call sites not found - skipping")
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

private fun BytecodePatchContext.applyIronSourceBridgeStrategy(logger: Logger, useIronSource: Boolean, instantReward: Boolean?): Boolean {
    val bridgeReady = IronSourceUnityRewardedAdIsReadyFingerprint.methodOrNull
    val bridgeShow = IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull
    if (!useIronSource || bridgeReady == null || bridgeShow == null) return false
    logger.info("Ads Free Rewards: IronSource patch succeeded")
    bridgeReady.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
    if (instantReward == true) {
        bridgeShow.addInstructions(0, """
            iget-object v0, p0, Lcom/ironsource/Ya;->k:Lcom/ironsource/Za;
            if-eqz v0, :morphe_ads_free_rewards_done
            iget-object p1, p0, Lcom/ironsource/Ya;->m:Lcom/ironsource/q6;
            invoke-interface {p1}, Lcom/ironsource/q6;->b()Lcom/unity3d/mediation/LevelPlayAdInfo;
            move-result-object p1
            invoke-interface {v0, p1}, Lcom/ironsource/Za;->onAdDisplayed(Lcom/unity3d/mediation/LevelPlayAdInfo;)V
            new-instance v1, Lcom/unity3d/mediation/rewarded/LevelPlayReward;
            const-string p2, "reward"
            const/4 p0, 0x1
            invoke-direct {v1, p2, p0}, Lcom/unity3d/mediation/rewarded/LevelPlayReward;-><init>(Ljava/lang/String;I)V
            invoke-interface {v0, v1, p1}, Lcom/ironsource/Za;->onAdRewarded(Lcom/unity3d/mediation/rewarded/LevelPlayReward;Lcom/unity3d/mediation/LevelPlayAdInfo;)V
            invoke-interface {v0, p1}, Lcom/ironsource/Za;->onAdClosed(Lcom/unity3d/mediation/LevelPlayAdInfo;)V
            :morphe_ads_free_rewards_done
            return-void
        """.trimIndent())
    }
    return true
}

private fun BytecodePatchContext.applyUnityAdsStrategy(logger: Logger, useUnityAds: Boolean, instantReward: Boolean?) {
    val adsShow = UnityRewardedAdShowFingerprint.methodOrNull ?: return
    if (!useUnityAds || instantReward != true) return
    adsShow.addInstructions(0, """
        invoke-interface {p3, p0}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
        invoke-interface {p3, p0}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
        sget-object v0, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
        invoke-interface {p3, p0, v0}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
        return-void
    """.trimIndent())
    logger.info("Ads Free Rewards: Unity Ads patch succeeded")
}

private fun BytecodePatchContext.applyUnityAdsV4Strategy(logger: Logger, useUnityAds: Boolean, instantReward: Boolean?) {
    if (!useUnityAds || instantReward != true) return
    val v4Show3 = UnityAdsV4Show3ArgFingerprint.methodOrNull
    if (v4Show3 != null) {
        v4Show3.addInstructions(0, """
            invoke-interface {p2, p1}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
            sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
            invoke-interface {p2, p1, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
            return-void
        """.trimIndent())
        logger.info("Ads Free Rewards: Unity Ads v4 patch succeeded (3-arg show)")
    }
    val v4Show4 = UnityAdsV4Show4ArgFingerprint.methodOrNull
    if (v4Show4 != null) {
        v4Show4.addInstructions(0, """
            invoke-interface {p3, p1}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
            sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
            invoke-interface {p3, p1, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
            return-void
        """.trimIndent())
        logger.info("Ads Free Rewards: Unity Ads v4 patch succeeded (4-arg show)")
    }
}

// Historical snapshots - each version is a frozen copy.
// Newer entries delegate to the current implementation for now; future
// bundle releases can diverge them with version-specific fixes.
private fun BytecodePatchContext.applyAdsFreeRewardsV1200(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.20.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1210(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.21.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1220(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.22.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1300(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.30.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1310(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.31.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1320(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.32.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1330(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.33.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1340(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.34.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1380(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.38.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1400(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.40.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
private fun BytecodePatchContext.applyAdsFreeRewardsV1410(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    logger.info("Ads Free Rewards v1.41.0 selected")
    applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
}
