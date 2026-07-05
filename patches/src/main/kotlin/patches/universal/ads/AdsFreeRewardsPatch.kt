package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    description = "Auto-claim rewarded ad rewards without watching ads. " +
            "WARNING: Enabling No Ads alongside this patch will prevent rewards from being claimed.",
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
        val hasLevelPlay = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull != null
        val hasIronSourceUnityBridge = IronSourceUnityRewardedAdIsReadyFingerprint.methodOrNull != null &&
            IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax && !hasUnityAds && !hasLevelPlay && !hasIronSourceUnityBridge) {
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
            // Register layout: registers=5, ins=4 → p0=v1(this), p1=v2(adUnitId), p2=v3, p3=v4.
            // Save p1 to v0 first so v2 can be used for string temps without corrupting adUnitId.
            unityShow.addInstructions(0, """
                move-object v0, p1
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
                return-void
            """.trimIndent())
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
            nativeShow.addInstructions(0, fireRewardedAdCallbacks())
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

    }
}
