package patches.universal.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import java.util.logging.Logger
import patches.universal.ads.util.fireHiddenCallbacks

private val logger = Logger.getLogger("patches.universal.ads.NoAdsPatch")

private fun BytecodePatchContext.injectOrSkip(
    fingerprint: Fingerprint,
    instructions: String,
) {
    val method = fingerprint.methodOrNull ?: return
    if (method.implementation == null) {
        logger.warning(
            "No Ads: skipping ${fingerprint.name}: method has no implementation",
        )
        return
    }
    method.addInstructions(0, instructions)
}

private fun BytecodePatchContext.returnVoid(fingerprint: Fingerprint) =
    injectOrSkip(fingerprint, "return-void")

private fun BytecodePatchContext.patchVoid(fingerprint: Fingerprint): Int {
    val name = fingerprint.name ?: return 0
    val params = fingerprint.parameters ?: emptyList()
    val ret = fingerprint.returnType ?: return 0
    val target = fingerprint.definingClass

    // Fast path: exact match has implementation
    val exact = fingerprint.methodOrNull
    if (exact != null && exact.implementation != null) {
        exact.addInstructions(0, "return-void")
        logger.info("No Ads: blocked $name (exact 1 impl)")
        return 1
    }

    // Generic show() with no args is too broad to scan (every Dialog has show()V)
    if (target != null && params.isEmpty() && name == "show" && ret == "V") {
        return 0
    }
    if (target != null && params.isEmpty() && name == "show" && ret == "Z") {
        // Facebook show()Z also generic
        return 0
    }

    var patched = 0
    classDefForEach { classDef ->
        for (m in classDef.methods) {
            if (m.name != name) continue
            if (m.returnType != ret) continue
            if (m.parameterTypes.map { it.toString() } != params) continue
            if (m.implementation == null) continue
            if (target != null) {
                val def = m.definingClass
                val isAd = def.contains("ads") || def.contains("applovin") || def.contains("ironsource") || def.contains("unity3d") || def.contains("vungle") || def.contains("facebook") || def.contains("bytedance") || def.contains("google/android/gms/ads") || def == target
                if (!isAd) continue
            }
            val mutableClass = mutableClassDefByOrNull(classDef.type) ?: return@classDefForEach
            val mutableMethod = mutableClass.methods.find { it.name == name && it.returnType == ret && it.parameterTypes.map { p -> p.toString() } == params } ?: return@classDefForEach
            if (mutableMethod.implementation == null) return@classDefForEach
            mutableMethod.addInstructions(0, "return-void")
            patched++
        }
    }
    if (patched > 0) logger.info("No Ads: blocked $name (${patched} impl(s)) via scan")
    return patched
}

private fun BytecodePatchContext.patchReturnFalse(fingerprint: Fingerprint): Int {
    val name = fingerprint.name ?: return 0
    val params = fingerprint.parameters ?: emptyList()
    val ret = fingerprint.returnType ?: return 0
    val target = fingerprint.definingClass

    val exact = fingerprint.methodOrNull
    if (exact != null && exact.implementation != null) {
        exact.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        logger.info("No Ads: forced $name -> false (exact 1 impl)")
        return 1
    }
    if (target != null && params.isEmpty() && name == "show") return 0

    var patched = 0
    classDefForEach { classDef ->
        for (m in classDef.methods) {
            if (m.name != name) continue
            if (m.returnType != ret) continue
            if (m.parameterTypes.map { it.toString() } != params) continue
            if (m.implementation == null) continue
            if (target != null) {
                val def = m.definingClass
                val isAd = def.contains("ads") || def.contains("applovin") || def.contains("ironsource") || def.contains("unity3d") || def.contains("vungle") || def.contains("facebook") || def.contains("bytedance") || def.contains("google/android/gms/ads") || def == target
                if (!isAd) continue
            }
            val mutableClass = mutableClassDefByOrNull(classDef.type) ?: return@classDefForEach
            val mutableMethod = mutableClass.methods.find { it.name == name && it.returnType == ret && it.parameterTypes.map { p -> p.toString() } == params } ?: return@classDefForEach
            if (mutableMethod.implementation == null) return@classDefForEach
            mutableMethod.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            patched++
        }
    }
    if (patched > 0) logger.info("No Ads: forced $name -> false (${patched} impl(s)) via scan")
    return patched
}

private fun BytecodePatchContext.patchWith(fingerprint: Fingerprint, smali: String): Int {
    val exact = fingerprint.methodOrNull
    if (exact != null && exact.implementation != null) {
        val rc = exact.implementation?.registerCount ?: 0
        if ((fingerprint === MaxInterstitialAdShowAdFingerprint || fingerprint === MaxAppOpenAdShowAdFingerprint || fingerprint === MaxRewardedAdShowAdFingerprint) && rc < 7) {
            logger.warning("No Ads: skipping ${fingerprint.name} in ${exact.definingClass}: register count $rc < 7")
            return 0
        }
        exact.addInstructions(0, smali)
        logger.info("No Ads: patched ${fingerprint.name} (exact 1 impl)")
        return 1
    }
    // fallback scan for these is risky (they need exact class reflection), skip
    return 0
}

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
        val detectionLogger = Logger.getLogger(this::class.java.name)

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
        val hasUnityAdsV4 = UnityAdsV4Show3ArgFingerprint.methodOrNull != null ||
            UnityAdsV4Show4ArgFingerprint.methodOrNull != null ||
            UnityRewardedAdShowFingerprint.methodOrNull != null
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
        val hasMyTarget = MyTargetBaseInterstitialShowFingerprint.methodOrNull != null
        val hasYandexRewarded = YandexUnityRewardedWrapperShowFingerprint.methodOrNull != null
        val hasYandexInterstitial = YandexUnityInterstitialWrapperShowFingerprint.methodOrNull != null
        val hasLevelPlay = LevelPlayRewardedAdIsReadyFingerprint.methodOrNull != null ||
            IronSourceLevelPlayFullScreenShowAdFingerprint.methodOrNull != null
        val hasHuawei = HuaweiRewardAdIsLoadedFingerprint.methodOrNull != null ||
            HuaweiRewardAdShowFingerprint.methodOrNull != null ||
            HuaweiInterstitialAdShowFingerprint.methodOrNull != null

        if (
            !hasMaxUnity &&
            !hasNativeMax &&
            !hasAdMob &&
            !hasUnityAdsV3 &&
            !hasUnityAdsV4 &&
            !hasIronSource &&
            !hasAppLovinLegacy &&
            !hasVungle &&
            !hasFacebook &&
            !hasPangle &&
            !hasLevelPlay &&
            !hasMyTarget &&
            !hasYandexRewarded &&
            !hasYandexInterstitial &&
            !hasHuawei
        ) {
            detectionLogger.warning(
                "Could not find supported ad SDK (MAX Unity, native MAX, AdMob, " +
                    "Unity Ads v3/v4, ironSource/LevelPlay, AppLovin, Vungle, " +
                    "Meta, Pangle, VK MyTarget, Yandex or Huawei Ads Kit). No changes applied.",
            )
            return@execute
        }

        // -- VK MyTarget / RuStore build --
        // MyTarget interstitial and rewarded classes inherit show(Context)
        // from the same base implementation. Use runtime type checks so the
        // individual No Ads toggles remain independent.
        if (hasMyTarget && (blockInterstitials == true || blockRewarded == true)) {
            val myTargetChecks = buildString {
                if (blockRewarded == true) {
                    appendLine("instance-of v0, p0, Lcom/my/target/ads/RewardedAd;")
                    appendLine("if-nez v0, :morphe_no_ads_mytarget_block")
                }
                if (blockInterstitials == true) {
                    appendLine("instance-of v0, p0, Lcom/my/target/ads/InterstitialAd;")
                    appendLine("if-nez v0, :morphe_no_ads_mytarget_block")
                }
                appendLine("goto :morphe_no_ads_mytarget_continue")
                appendLine(":morphe_no_ads_mytarget_block")
                appendLine("return-void")
                appendLine(":morphe_no_ads_mytarget_continue")
            }
            injectOrSkip(MyTargetBaseInterstitialShowFingerprint, myTargetChecks.trim())
        }

        if (blockRewarded == true) {
            returnVoid(YandexUnityRewardedWrapperShowFingerprint)
        }
        if (blockInterstitials == true) {
            returnVoid(YandexUnityInterstitialWrapperShowFingerprint)
        }

        var totalPatched = 0

        // -- Huawei Ads Kit / Petal Ads --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(HuaweiInterstitialAdShowFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchReturnFalse(HuaweiRewardAdIsLoadedFingerprint)
            totalPatched += patchVoid(HuaweiRewardAdShowFingerprint)
        }

        // -- MAX Unity wrapper --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(ShowInterstitialFingerprint)
        }
        if (blockAppOpen == true) {
            totalPatched += patchVoid(ShowAppOpenAdFingerprint)
        }
        if (blockBanners == true) {
            totalPatched += patchVoid(ShowBannerFingerprint)
            totalPatched += patchVoid(StartBannerAutoRefreshFingerprint)
        }
        if (blockMRec == true) {
            totalPatched += patchVoid(ShowMRecFingerprint)
            totalPatched += patchVoid(StartMRecAutoRefreshFingerprint)
        }

        // -- Native MAX (non-Unity) --
        if (blockInterstitials == true) {
            totalPatched += patchWith(
                MaxInterstitialAdShowAdFingerprint,
                fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxInterstitialAd;"),
            )
        }
        if (blockAppOpen == true) {
            totalPatched += patchWith(
                MaxAppOpenAdShowAdFingerprint,
                fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxAppOpenAd;"),
            )
        }
        if (blockBanners == true || blockMRec == true) {
            totalPatched += patchVoid(MaxAdViewStartAutoRefreshFingerprint)
        }

        // -- AdMob (Google Mobile Ads) --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(AdMobInterstitialShowFingerprint)
            totalPatched += patchVoid(AdMobLegacyInterstitialShowFingerprint)
        }
        if (blockAppOpen == true) {
            totalPatched += patchVoid(AdMobAppOpenShowFingerprint)
            totalPatched += patchVoid(AdMobAppOpenLoadFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchVoid(AdMobRewardedShowFingerprint)
            totalPatched += patchVoid(AdMobLegacyRewardedVideoShowFingerprint)
        }

        // -- Rewarded ads --
        if (blockRewarded == true) {
            totalPatched += patchReturnFalse(IsRewardedAdReadyFingerprint)
            totalPatched += patchVoid(ShowRewardedAdFingerprint)
            totalPatched += patchReturnFalse(MaxRewardedAdIsReadyFingerprint)
            totalPatched += patchWith(
                MaxRewardedAdShowAdFingerprint,
                fireHiddenCallbacks("Lcom/applovin/mediation/ads/MaxRewardedAd;"),
            )
            totalPatched += patchReturnFalse(LevelPlayRewardedAdIsReadyFingerprint)
            totalPatched += patchReturnFalse(IronSourceUnityRewardedAdIsReadyFingerprint)
            totalPatched += patchVoid(IronSourceLevelPlayFullScreenShowAdFingerprint)
        }

        // -- Unity Ads v3 (legacy) and v4 / RewardedAd --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(UnityAdsV3Show2ArgFingerprint)
            totalPatched += patchVoid(UnityAdsV3ShowOptionsFingerprint)
        }
        if (blockInterstitials == true || blockRewarded == true) {
            totalPatched += patchVoid(UnityAdsV4Show3ArgFingerprint)
            totalPatched += patchVoid(UnityAdsV4Show4ArgFingerprint)
            totalPatched += patchVoid(UnityRewardedAdShowFingerprint)
        }

        // -- ironSource (LevelPlay) public API --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(IronSourceShowDemandOnlyInterstitialFingerprint)
            totalPatched += patchVoid(IronSourceShowInterstitialFingerprint)
            totalPatched += patchVoid(IronSourceShowInterstitialActivityFingerprint)
            totalPatched += patchVoid(IronSourceShowInterstitialActivityPlacementFingerprint)
            totalPatched += patchVoid(IronSourceShowInterstitialPlacementFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchVoid(IronSourceShowDemandOnlyRewardedVideoFingerprint)
            totalPatched += patchVoid(IronSourceShowRewardedVideoFingerprint)
            totalPatched += patchVoid(IronSourceShowRewardedVideoActivityFingerprint)
            totalPatched += patchVoid(IronSourceShowRewardedVideoActivityPlacementFingerprint)
            totalPatched += patchVoid(IronSourceShowRewardedVideoPlacementFingerprint)
        }

        // -- AppLovin legacy (direct SDK, non-MAX) --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(AppLovinInterstitialDialogShowFingerprint)
            totalPatched += patchVoid(AppLovinInterstitialDialogShowAndRenderFingerprint)
        }
        if (blockBanners == true) {
            totalPatched += patchVoid(AppLovinAdViewLoadNextAdFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchVoid(AppLovinIncentivizedShow4ListenerFingerprint)
            totalPatched += patchVoid(AppLovinIncentivizedShow5ListenerFingerprint)
        }

        // -- Vungle --
        if (blockInterstitials == true || blockRewarded == true) {
            totalPatched += patchVoid(VungleBaseFullscreenAdLoadFingerprint)
        }

        // -- Meta Audience Network (facebook/ads) --
        if (blockInterstitials == true) {
            totalPatched += patchReturnFalse(FacebookInterstitialAdShowFingerprint)
            totalPatched += patchReturnFalse(FacebookInterstitialAdShowConfigFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchReturnFalse(FacebookRewardedVideoAdShowFingerprint)
            totalPatched += patchReturnFalse(FacebookRewardedVideoAdShowConfigFingerprint)
            totalPatched += patchReturnFalse(FacebookRewardedInterstitialShowFingerprint)
            totalPatched += patchReturnFalse(FacebookRewardedInterstitialShowConfigFingerprint)
        }

        // -- Pangle (bytedance) --
        if (blockInterstitials == true) {
            totalPatched += patchVoid(PangleInterstitialShowFingerprint)
        }
        if (blockAppOpen == true) {
            totalPatched += patchVoid(PangleAppOpenShowFingerprint)
        }
        if (blockRewarded == true) {
            totalPatched += patchVoid(PangleRewardedShowFingerprint)
        }

        if (totalPatched == 0) {
            detectionLogger.warning("No Ads: no patchable ad methods found for selected options. Try enabling more categories or the app uses an unsupported SDK.")
        } else {
            detectionLogger.info("No Ads: patched $totalPatched method(s) in total")
        }
    }
}
