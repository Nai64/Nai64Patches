package patches.universal.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

// Noop every void overload matched by the fingerprint. Init methods only need
// their own param slots for a leading return-void, so no frame expansion is
// required; non-void overloads are never touched.
private fun BytecodePatchContext.killInit(
    logger: Logger,
    fingerprint: Fingerprint,
    label: String,
): Int {
    var patched = 0
    try {
        val matches: List<Match> = try {
            with(this@killInit) { fingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in matches) {
            try {
                val method = m.method
                if (method.implementation == null) continue
                if (method.returnType != "V") continue
                method.addInstructions(0, "return-void")
                patched++
            } catch (_: Exception) {}
        }
        if (patched > 0) {
            logger.info("Disable Ad SDK Init: killed $label init ($patched overload(s))")
            return patched
        }
    } catch (_: Exception) {}
    try {
        val single = fingerprint.methodOrNull
        if (single?.implementation != null && single.returnType == "V") {
            single.addInstructions(0, "return-void")
            logger.info("Disable Ad SDK Init: killed $label init (exact 1 impl)")
            return 1
        }
    } catch (_: Exception) {}
    return 0
}

@Suppress("unused")
val disableAdSdkInitPatch = bytecodePatch(
    name = "Disable Ad SDK Init",
    description = "Stops ad SDKs from initializing at startup (no tracking, faster launch). Aggressive: apps gating content on init callbacks may hang. Do not combine with Ads Free Rewards, which needs live SDKs.",
    default = false,
) {
    category("Disable")
    val killAdMob by booleanOption(
        title = "AdMob",
        default = false,
        key = "killAdMob",
        description = "Kill MobileAds.initialize.",
    )
    val killMax by booleanOption(
        title = "MAX",
        default = false,
        key = "killMax",
        description = "Kill AppLovinSdk.initializeSdk.",
    )
    val killUnityAds by booleanOption(
        title = "Unity Ads",
        default = false,
        key = "killUnityAds",
        description = "Kill UnityAds.initialize.",
    )
    val killIronSource by booleanOption(
        title = "ironSource",
        default = false,
        key = "killIronSource",
        description = "Kill IronSource.init.",
    )
    val killPangle by booleanOption(
        title = "Pangle",
        default = false,
        key = "killPangle",
        description = "Kill TTAdSdk.init.",
    )
    val killMeta by booleanOption(
        title = "Meta",
        default = false,
        key = "killMeta",
        description = "Kill AudienceNetworkAds.initialize.",
    )
    val killVungle by booleanOption(
        title = "Vungle",
        default = false,
        key = "killVungle",
        description = "Kill Vungle init (new + legacy API).",
    )
    val killHuawei by booleanOption(
        title = "Huawei Ads",
        default = false,
        key = "killHuawei",
        description = "Kill HwAds.init.",
    )
    val killYandex by booleanOption(
        title = "Yandex",
        default = false,
        key = "killYandex",
        description = "Kill Yandex MobileAds.initialize.",
    )
    val killOthers by booleanOption(
        title = "Other networks",
        default = false,
        key = "killOthers",
        description = "Kill MyTarget, StartApp, Chartboost, InMobi and Mintegral init.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var totalPatched = 0

        if (killAdMob == true) totalPatched += killInit(logger, AdMobInitFingerprint, "AdMob")
        if (killMax == true) totalPatched += killInit(logger, MaxInitSdkFingerprint, "MAX")
        if (killUnityAds == true) totalPatched += killInit(logger, UnityAdsInitFingerprint, "Unity Ads")
        if (killIronSource == true) totalPatched += killInit(logger, IronSourceInitFingerprint, "ironSource")
        if (killPangle == true) totalPatched += killInit(logger, PangleInitFingerprint, "Pangle")
        if (killMeta == true) totalPatched += killInit(logger, MetaInitFingerprint, "Meta")
        if (killVungle == true) {
            totalPatched += killInit(logger, VungleInitFingerprint, "Vungle")
            totalPatched += killInit(logger, VungleLegacyInitFingerprint, "Vungle legacy")
        }
        if (killHuawei == true) totalPatched += killInit(logger, HuaweiInitFingerprint, "Huawei")
        if (killYandex == true) totalPatched += killInit(logger, YandexInitFingerprint, "Yandex")
        if (killOthers == true) {
            totalPatched += killInit(logger, MyTargetInitFingerprint, "MyTarget")
            totalPatched += killInit(logger, StartAppInitFingerprint, "StartApp")
            totalPatched += killInit(logger, ChartboostInitFingerprint, "Chartboost")
            totalPatched += killInit(logger, InMobiInitFingerprint, "InMobi")
            totalPatched += killInit(logger, MintegralInitFingerprint, "Mintegral")
        }

        if (totalPatched == 0) {
            logger.warning("Disable Ad SDK Init: no SDK init methods found for the enabled networks, or no network enabled. No changes applied.")
        } else {
            logger.info("Disable Ad SDK Init: killed $totalPatched init method(s) in total")
        }
    }
}
