package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.fireRewardedAdCallbacks
import java.util.logging.Logger

@Suppress("unused")
val adsFreeRewardsPatch = bytecodePatch(
    name = "Ads Free Rewards",
    description = "Auto-claim rewarded ad rewards without watching ads. Supports MAX Unity, native MAX. " +
            "Enabling No Ads will prevent rewards from being claimed.",
    default = false,
) {
    execute {
        // ── Bail out early if no supported SDK is present ──
        val hasMaxUnity = ShowRewardedAdFingerprint.methodOrNull != null &&
            IsRewardedAdReadyFingerprint.methodOrNull != null
        val hasNativeMax = MaxRewardedAdIsReadyFingerprint.methodOrNull != null &&
            MaxRewardedAdShowAdFingerprint.methodOrNull != null

        if (!hasMaxUnity && !hasNativeMax) {
            return@execute Logger.getLogger(this::class.java.name)
                .warning("Could not find supported ad SDK (MAX Unity or native MAX). No changes applied.")
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
            unityShow.addInstructions(0, """
                new-instance v0, Lorg/json/JSONObject;
                invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V
                const-string v1, "name"
                const-string v2, "OnRewardedAdReceivedRewardEvent"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "adUnitId"
                invoke-static {v0, v1, p1}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "adFormat"
                const-string v2, "rewarded"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "rewardLabel"
                const-string v2, "reward"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "rewardAmount"
                const-string v2, "1"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                invoke-static {v0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                new-instance v0, Lorg/json/JSONObject;
                invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V
                const-string v1, "name"
                const-string v2, "OnRewardedAdHiddenEvent"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "adUnitId"
                invoke-static {v0, v1, p1}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                const-string v1, "adFormat"
                const-string v2, "rewarded"
                invoke-static {v0, v1, v2}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                invoke-static {v0}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
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

        // ── No supported SDK found — silently skip ──
    }
}
