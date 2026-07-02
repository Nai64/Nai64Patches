package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    description = "Auto-claim rewarded ad rewards without watching ads. Supports MAX Unity, native MAX, Unity Ads. " +
            "WARNING: Enabling No Ads alongside this patch will prevent rewards from being claimed.",
    default = false,
) {
    execute {
        // ── Bail out early if no supported SDK is present ──
        val hasMaxUnity = ShowRewardedAdFingerprint.methodOrNull != null &&
            IsRewardedAdReadyFingerprint.methodOrNull != null
        val hasNativeMax = MaxRewardedAdIsReadyFingerprint.methodOrNull != null &&
            MaxRewardedAdShowAdFingerprint.methodOrNull != null
        val hasUnityAds = UnityRewardedAdShowFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax && !hasUnityAds) {
            return@execute Logger.getLogger(this::class.java.name)
                .warning("Could not find supported ad SDK (MAX Unity, native MAX, or Unity Ads). No changes applied.")
        }

        // ── Strategy 1: MAX Unity wrapper ──
        val unityShow = ShowRewardedAdFingerprint.methodOrNull
        val unityReady = IsRewardedAdReadyFingerprint.methodOrNull
        if (unityShow != null && unityReady != null) {
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
            nativeReady.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """.trimIndent())

            // Use reflection to find the MaxRewardedAdListener field and fire
            // callbacks directly (onAdDisplayed → onRewardedVideoStarted →
            // onUserRewarded → onRewardedVideoCompleted → onAdHidden).
            // This avoids crashes from simply NOP'ing showAd().
            nativeShow.addInstructions(0, fireRewardedAdCallbacks())
            return@execute
        }

        // ── Strategy 3: Unity Ads RewardedAd ──
        val adsShow = UnityRewardedAdShowFingerprint.methodOrNull
        if (adsShow != null) {
            // Only patch show() — do NOT patch load() so the real ad loads
            // silently in the background (prevents Unity Ads error 628).
            adsShow.addInstructions(0, """
                invoke-interface {p3, p0}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
                invoke-interface {p3, p0}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
                sget-object v0, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
                invoke-interface {p3, p0, v0}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
                return-void
            """.trimIndent())
            return@execute
        }

        // ── No supported SDK found — silently skip ──
    }
}
