package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.patch.BytecodePatchContext
import patches.universal.ads.util.cloneMutableAndPreserveParameters
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    description = "Skip rewarded ads and claim rewards instantly",
    default = false,
) {
    val patchVersion by stringOption(
        key = "patchVersion",
        default = "1.19.0",
        title = "Patch version",
        description = "Which implementation of the patch to use",
        values = linkedMapOf(
            "1.1.0 (Original)" to "1.1.0",
            "1.15.0" to "1.15.0",
            "1.16.0" to "1.16.0",
            "1.17.0" to "1.17.0",
            "1.18.0" to "1.18.0",
            "1.18.1" to "1.18.1",
            "1.19.0 (Current)" to "1.19.0",
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
            else -> applyAdsFreeRewardsV1190(logger, rewardStrategy, instantReward)
        }
        if (fakeAdAvailability == true) {
            forceAdAvailability(logger)
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
private fun BytecodePatchContext.forceAdAvailability(logger: Logger) {
    val targets = listOf(
        "Unity Ads Advertisement.isReady()" to UnityAdsAdvertisementIsReadyFingerprint.methodOrNull,
        "Unity Ads Advertisement.isReady(placement)" to UnityAdsAdvertisementIsReadyPlacementFingerprint.methodOrNull,
        "Unity Ads UnityAds.isReady()" to UnityAdsSdkIsReadyFingerprint.methodOrNull,
        "ironSource isRewardedVideoAvailable()" to IronSourceIsRewardedVideoAvailableFingerprint.methodOrNull,
        "ironSource isInterstitialReady()" to IronSourceIsInterstitialReadyFingerprint.methodOrNull,
        "AppLovin MAX InterstitialAd.isReady()" to MaxInterstitialAdIsReadyFingerprint.methodOrNull,
        "AppLovin MAX AppOpenAd.isReady()" to MaxAppOpenAdIsReadyFingerprint.methodOrNull,
        "Yandex/MyTarget rewarded mediation isLoaded()" to YandexMyTargetRewardedIsLoadedFingerprint.methodOrNull,
        "Yandex/MyTarget interstitial mediation isLoaded()" to YandexMyTargetInterstitialIsLoadedFingerprint.methodOrNull,
    )
    for ((label, method) in targets) {
        method ?: continue
        method.addInstructions(
            0,
            """
            const/4 v0, 0x1
            return v0
            """.trimIndent(),
        )
        logger.info("Ads Free Rewards: faked availability for $label")
    }
}

private fun BytecodePatchContext.applyAdsFreeRewardsV1190(logger: Logger, rewardStrategy: String?, instantReward: Boolean?) {
    val strategy = rewardStrategy
    val useMax = strategy == "auto" || strategy == "max"
    val useUnityAds = strategy == "auto" || strategy == "unityAds"
    val useIronSource = strategy == "auto" || strategy == "ironSource"
    val useRustore = strategy == "auto" || strategy == "rustore"

    // -- SDK detection --
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

    if (!hasMaxUnity && !hasNativeMax && !hasUnityAds && !hasUnityAdsV4 && !hasLevelPlay && !hasIronSourceUnityBridge && !hasMyTarget && !hasYandexUnityRewarded) {
        return
    }

    // -- Strategy 3c: VK MyTarget used by the RuStore build --
    // MyTarget's RewardedAd inherits show(Context) from BaseInterstitialAd.
    // Detect the runtime object so ordinary MyTarget interstitials continue
    // through the original implementation, while rewarded callbacks are
    // delivered without displaying an ad.
    val myTargetShow = MyTargetBaseInterstitialShowFingerprint.methodOrNull
    if (useRustore && instantReward == true && myTargetShow != null) {
        logger.info("RuStore / VK MyTarget rewarded patch succeeded")
        myTargetShow.addInstructions(0, """
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
    }

    // Yandex Unity wrapper used by the Rustore build. The wrapper forwards
    // events through its private listener object, so normalize the reward
    // callback and synthesize the shown -> rewarded -> dismissed lifecycle.
    val yandexRewardedShow = YandexUnityRewardedWrapperShowFingerprint.methodOrNull
    val yandexOnRewarded = YandexUnityRewardedListenerOnRewardedFingerprint.methodOrNull
    if (useRustore && instantReward == true && yandexRewardedShow != null && yandexOnRewarded != null) {
        logger.info("RuStore / Yandex Unity rewarded patch succeeded")
        yandexOnRewarded.addInstructions(0, """
            iget-object v0, p0, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;
            if-eqz v0, :morphe_rustore_yandex_reward_done
            const/4 v1, 0x1
            const-string p1, "default"
            invoke-interface {v0, v1, p1}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/UnityRewardedAdListener;->onRewarded(ILjava/lang/String;)V
            :morphe_rustore_yandex_reward_done
            return-void
        """.trimIndent())
        yandexRewardedShow.addInstructions(0, """
            iget-object v0, p0, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/RewardedAdWrapper;->b:Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;
            if-eqz v0, :morphe_rustore_yandex_show_done
            invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdShown()V
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onRewarded(Lcom/yandex/mobile/ads/rewarded/Reward;)V
            invoke-virtual {v0}, Lcom/yandex/mobile/ads/unity/wrapper/rewarded/a;->onAdDismissed()V
            :morphe_rustore_yandex_show_done
            return-void
        """.trimIndent())
    }

    // -- Strategy 1: MAX Unity wrapper --
    val unityShow = ShowRewardedAdFingerprint.methodOrNull
    val unityReady = IsRewardedAdReadyFingerprint.methodOrNull
    if (useMax && unityShow != null && unityReady != null) {
        logger.info("MAX Unity Ad wrapper patch succeeded")
        // Force isRewardedAdReady to always return true
        unityReady.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // Replace showRewardedAd with JSONObject + forwardUnityEvent.
        // Uses JsonUtils.putString (avoids JSONException), then calls
        // forwardUnityEvent to push through the MAX SDK callback pipeline.
        // The method is cloned with extra registers that hold copies of the
        // parameters (see BytecodeUtils.cloneMutableAndPreserveParameters),
        // so the injection only uses v0 + p0/p1/p2 and works with ANY
        // register layout (e.g. .registers 3 like Crowd Champs' showAd path).
        if (instantReward == true) {
            val showClass = ShowRewardedAdFingerprint.classDefOrNull
            val clonedShow = unityShow.cloneMutableAndPreserveParameters(showClass!!)
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
        }

        // Patch loadRewardedAd to fire OnRewardedAdLoadedEvent via forwardUnityEvent.
        // When the game C# IL2CPP side calls MaxSdk.LoadRewardedAd() and subscribes to
        // OnRewardedAdLoadedEvent, this synthetic event transitions the reward state
        // machine from "loading" to "loaded", enabling the reward button to work.
        // Same clone + preserve-parameters trick as showRewardedAd: the injection
        // only uses v0/v1 + p0/p1, valid for ANY register layout (Crowd Champs
        // compiles this method with .registers 2, which previously broke the
        // v2/v3-based injection).
        val unityLoad = LoadRewardedAdFingerprint.methodOrNull
        if (unityLoad != null) {
            logger.info("MAX Unity loadRewardedAd patching")
            val loadClass = LoadRewardedAdFingerprint.classDefOrNull
            val clonedLoad = unityLoad.cloneMutableAndPreserveParameters(loadClass!!)
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
        return
    }

    // -- Strategy 2: Native MAX (non-Unity) --
    val nativeReady = MaxRewardedAdIsReadyFingerprint.methodOrNull
    val nativeShow = MaxRewardedAdShowAdFingerprint.methodOrNull
    if (useMax && nativeReady != null && nativeShow != null) {
        logger.info("native MAX patch succeeded")
        nativeReady.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // Use reflection to find the MaxRewardedAdListener field and fire
        // callbacks directly (onAdDisplayed -> onRewardedVideoStarted ->
        // onUserRewarded -> onRewardedVideoCompleted -> onAdHidden).
        // This avoids crashes from simply NOP'ing showAd().
        // The reflection loop needs 7 registers (v0-v6); skip methods with
        // fewer to avoid out-of-range register failures on reassembly.
        if (instantReward == true && (nativeShow.implementation?.registerCount ?: 0) >= 7) {
            nativeShow.addInstructions(0, fireRewardedAdCallbacks())
        } else if (instantReward == true) {
            logger.warning(
                "Skipping native MAX showAd() patch: " +
                    "register count ${nativeShow.implementation?.registerCount} < 7"
            )
        }
        // Do NOT return - let subsequent strategies run for games where the
        // MAX showAd patch may not intercept the actual ad path (e.g. IL2CPP
        // games with ProGuard-broken showAd()V, or games routing through
        // LevelPlay / ironSource / Unity Ads instead).
    }

    // -- Strategy 3: LevelPlay RewardedAd (ironSource mediation) --
    // Forces isAdReady() to return true. showAd() is NOT patched here;
    // the call flows through to the ironSource Unity adapter which
    // invokes com.unity3d.ads.RewardedAd.show(), which Strategy 4 patches.
    val levelPlayReady = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull
    if (useIronSource && levelPlayReady != null) {
        logger.info("LevelPlay patch succeeded")
        levelPlayReady.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
        // Continue to Strategy 4 to also patch RewardedAd.show()
    }

    // Strategy 3b: ironSource Unity bridge backed by LevelPlay.
    // Pickcrafter uses this bridge instead of MAX. Force the Unity-facing
    // ready check, then intercept the shared fullscreen show path and fire
    // the bridge listener lifecycle directly.
    val bridgeReady = IronSourceUnityRewardedAdIsReadyFingerprint.methodOrNull
    val bridgeShow = IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull
    if (useIronSource && bridgeReady != null && bridgeShow != null) {
        logger.info("IronSource patch succeeded")
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
        return
    }

    // -- Strategy 4: Unity Ads RewardedAd. --
    val adsShow = UnityRewardedAdShowFingerprint.methodOrNull
    if (useUnityAds && adsShow != null && instantReward == true) {
        logger.info("Unity Ads patch succeeded")
        // Only patch show() - do NOT patch load() so the real ad loads
        // silently in the background (prevents Unity Ads error 628).
        adsShow.addInstructions(0, """
            invoke-interface {p3, p0}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
            invoke-interface {p3, p0}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
            sget-object v0, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
            invoke-interface {p3, p0, v0}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
            return-void
        """.trimIndent())
    }

    // -- Strategy 5: Unity Ads SDK v4 (UnityAds.show + IUnityAdsShowListener). --
    // IL2CPP games using the new Unity Ads 4.x native engine (e.g. Coin Flip
    // Master) call UnityAds.show(Activity, placementId, listener) instead of
    // the legacy RewardedAd API. Fire onUnityAdsShowStart + onUnityAdsShowComplete
    // (COMPLETED) so the C# side grants the reward without showing a real ad.
    // Register layout (static methods): p0=Activity, p1=placementId,
    // p2=options (4-arg only), listener is the last parameter.
    if (useUnityAds && instantReward == true) {
        val v4Show3 = UnityAdsV4Show3ArgFingerprint.methodOrNull
        if (v4Show3 != null) {
            logger.info("Unity Ads v4 patch succeeded (3-arg show)")
            v4Show3.addInstructions(0, """
                invoke-interface {p2, p1}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                invoke-interface {p2, p1, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                return-void
            """.trimIndent())
        }
        val v4Show4 = UnityAdsV4Show4ArgFingerprint.methodOrNull
        if (v4Show4 != null) {
            logger.info("Unity Ads v4 patch succeeded (4-arg show)")
            v4Show4.addInstructions(0, """
                invoke-interface {p3, p1}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                invoke-interface {p3, p1, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                return-void
            """.trimIndent())
        }
    }
}
