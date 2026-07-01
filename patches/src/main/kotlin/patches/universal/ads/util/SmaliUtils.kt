package patches.universal.ads.util

/**
 * Generates Smali bytecode that uses reflection to find the
 * MaxRewardedAdListener field on `this`, then fires the full callback
 * chain: onAdDisplayed → onRewardedVideoStarted → onUserRewarded →
 * onRewardedVideoCompleted → onAdHidden.
 *
 * Designed to replace showAd() in MaxRewardedAd without crashing
 * the game (game gets the reward callbacks it expects).
 */
fun fireRewardedAdCallbacks(): String = """
    const-class v0, Lcom/applovin/mediation/ads/MaxRewardedAd;
    invoke-virtual {v0}, Ljava/lang/Class;->getDeclaredFields()[Ljava/lang/reflect/Field;
    move-result-object v0
    array-length v1, v0
    const/4 v2, 0x0
    :loop_ck
    if-lt v2, v1, :loop_done
    aget-object v3, v0, v2
    invoke-virtual {v3}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;
    move-result-object v4
    const-class v5, Lcom/applovin/mediation/MaxRewardedAdListener;
    invoke-virtual {v5, v4}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z
    move-result v6
    if-eqz v6, :found
    const-class v5, Lcom/applovin/mediation/MaxAdListener;
    invoke-virtual {v5, v4}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z
    move-result v6
    if-eqz v6, :found
    add-int/lit8 v2, v2, 0x1
    goto :loop_ck
    :found
    const/4 v4, 0x1
    invoke-virtual {v3, v4}, Ljava/lang/reflect/Field;->setAccessible(Z)V
    invoke-virtual {v3, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v4
    if-nez v4, :loop_done
    check-cast v4, Lcom/applovin/mediation/MaxRewardedAdListener;
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxRewardedAdListener;->onAdDisplayed(Lcom/applovin/mediation/MaxAd;)V
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxRewardedAdListener;->onRewardedVideoStarted(Lcom/applovin/mediation/MaxAd;)V
    const/4 v5, 0x1
    const-string v6, "reward"
    invoke-static {v5, v6}, Lcom/applovin/mediation/MaxReward;->create(ILjava/lang/String;)Lcom/applovin/mediation/MaxReward;
    move-result-object v5
    invoke-interface {v4, p0, v5}, Lcom/applovin/mediation/MaxRewardedAdListener;->onUserRewarded(Lcom/applovin/mediation/MaxAd;Lcom/applovin/mediation/MaxReward;)V
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxRewardedAdListener;->onRewardedVideoCompleted(Lcom/applovin/mediation/MaxAd;)V
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxRewardedAdListener;->onAdHidden(Lcom/applovin/mediation/MaxAd;)V
    :loop_done
    return-void
""".trimIndent()

/**
 * Generates Smali bytecode that uses reflection to find the
 * MaxAdListener field on `this`, then fires onAdDisplayed + onAdHidden.
 *
 * Designed to replace showAd() in MaxInterstitialAd / MaxAppOpenAd
 * so the game proceeds without actually showing an ad.
 */
fun fireHiddenCallbacks(className: String): String = """
    const-class v0, $className;
    invoke-virtual {v0}, Ljava/lang/Class;->getDeclaredFields()[Ljava/lang/reflect/Field;
    move-result-object v0
    array-length v1, v0
    const/4 v2, 0x0
    :loop_ck
    if-lt v2, v1, :loop_done
    aget-object v3, v0, v2
    invoke-virtual {v3}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;
    move-result-object v4
    const-class v5, Lcom/applovin/mediation/MaxAdListener;
    invoke-virtual {v5, v4}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z
    move-result v6
    if-eqz v6, :found
    add-int/lit8 v2, v2, 0x1
    goto :loop_ck
    :found
    const/4 v4, 0x1
    invoke-virtual {v3, v4}, Ljava/lang/reflect/Field;->setAccessible(Z)V
    invoke-virtual {v3, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v4
    if-nez v4, :loop_done
    check-cast v4, Lcom/applovin/mediation/MaxAdListener;
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxAdListener;->onAdDisplayed(Lcom/applovin/mediation/MaxAd;)V
    invoke-interface {v4, p0}, Lcom/applovin/mediation/MaxAdListener;->onAdHidden(Lcom/applovin/mediation/MaxAd;)V
    :loop_done
    return-void
""".trimIndent()
