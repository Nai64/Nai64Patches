package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.cloneMutableAndPreserveParameters
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // ── SDK detection ──
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

        if (!hasMaxUnity && !hasNativeMax && !hasUnityAds && !hasUnityAdsV4 && !hasLevelPlay && !hasIronSourceUnityBridge) {
            return@execute
        }

        // ── Strategy 1: MAX Unity wrapper ──
        val unityShow = ShowRewardedAdFingerprint.methodOrNull
        val unityReady = IsRewardedAdReadyFingerprint.methodOrNull
        if (unityShow != null && unityReady != null) {
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
            return@execute
        }

        // ── Strategy 2: Native MAX (non-Unity) ──
        val nativeReady = MaxRewardedAdIsReadyFingerprint.methodOrNull
        val nativeShow = MaxRewardedAdShowAdFingerprint.methodOrNull
        if (nativeReady != null && nativeShow != null) {
            logger.info("native MAX patch succeeded")
            nativeReady.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """.trimIndent())

            // Use reflection to find the MaxRewardedAdListener field and fire
            // callbacks directly (onAdDisplayed → onRewardedVideoStarted →
            // onUserRewarded → onRewardedVideoCompleted → onAdHidden).
            // This avoids crashes from simply NOP'ing showAd().
            // The reflection loop needs 7 registers (v0-v6); skip methods with
            // fewer to avoid out-of-range register failures on reassembly.
            if ((nativeShow.implementation?.registerCount ?: 0) >= 7) {
                nativeShow.addInstructions(0, fireRewardedAdCallbacks())
            } else {
                logger.warning(
                    "Skipping native MAX showAd() patch: " +
                        "register count ${nativeShow.implementation?.registerCount} < 7"
                )
            }
            // Do NOT return — let subsequent strategies run for games where the
            // MAX showAd patch may not intercept the actual ad path (e.g. IL2CPP
            // games with ProGuard-broken showAd()V, or games routing through
            // LevelPlay / ironSource / Unity Ads instead).
        }

        // ── Strategy 3: LevelPlay RewardedAd (ironSource mediation) ──
        // Forces isAdReady() to return true. showAd() is NOT patched here;
        // the call flows through to the ironSource Unity adapter which
        // invokes com.unity3d.ads.RewardedAd.show(), which Strategy 4 patches.
        val levelPlayReady = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull
        if (levelPlayReady != null) {
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
        if (bridgeReady != null && bridgeShow != null) {
            logger.info("IronSource patch succeeded")
            bridgeReady.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """.trimIndent())
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
            return@execute
        }

        // Strategy 4: Unity Ads RewardedAd.
        val adsShow = UnityRewardedAdShowFingerprint.methodOrNull
        if (adsShow != null) {
            logger.info("Unity Ads patch succeeded")
            // Only patch show() — do NOT patch load() so the real ad loads
            // silently in the background (prevents Unity Ads error 628).
            adsShow.addInstructions(0, """
                invoke-interface {p3, p0}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
                invoke-interface {p3, p0}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
                sget-object v0, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
                invoke-interface {p3, p0, v0}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
                return-void
            """.trimIndent())
        }

        // Strategy 5: Unity Ads SDK v4 (UnityAds.show + IUnityAdsShowListener).
        // IL2CPP games using the new Unity Ads 4.x native engine (e.g. Coin Flip
        // Master) call UnityAds.show(Activity, placementId, listener) instead of
        // the legacy RewardedAd API. Fire onUnityAdsShowStart + onUnityAdsShowComplete
        // (COMPLETED) so the C# side grants the reward without showing a real ad.
        // Register layout (static methods): p0=Activity, p1=placementId,
        // p2=options (4-arg only), listener is the last parameter.
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
