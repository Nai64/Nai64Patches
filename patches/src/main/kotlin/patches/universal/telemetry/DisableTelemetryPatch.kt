package patches.universal.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private fun app.morphe.patcher.patch.BytecodePatchContext.safeEarlyReturn(fingerprint: app.morphe.patcher.Fingerprint): Boolean {
    val method = fingerprint.methodOrNull ?: return false
    val impl = method.implementation ?: return false
    if (impl.registerCount < 1) return false
    val ret = method.returnType
    when {
        ret == "V" -> method.addInstruction(0, "return-void")
        ret.startsWith("L") || ret.startsWith("[") -> method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
        ret == "Z" || ret == "I" || ret == "B" || ret == "S" || ret == "C" -> method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        ret == "J" -> method.addInstructions(0, "const-wide v0, 0x0\nreturn-wide v0")
        ret == "F" -> method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        ret == "D" -> method.addInstructions(0, "const-wide v0, 0x0\nreturn-wide v0")
        else -> method.addInstruction(0, "return-void")
    }
    return true
}

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable Telemetry",
    description = "Block analytics and tracking",
    default = false,
) {
    val blockFirebase by booleanOption(
        title = "Block Firebase Analytics",
        default = true,
        key = "blockFirebase",
        description = "Google's analytics and crash reporting suite",
    )
    val blockAppsFlyer by booleanOption(
        title = "Block AppsFlyer",
        default = true,
        key = "blockAppsFlyer",
        description = "Mobile attribution and marketing analytics",
    )
    val blockAdjust by booleanOption(
        title = "Block Adjust",
        default = true,
        key = "blockAdjust",
        description = "Mobile attribution analytics",
    )
    val blockAmplitude by booleanOption(
        title = "Block Amplitude",
        default = true,
        key = "blockAmplitude",
        description = "Product analytics platform",
    )
    val blockMixpanel by booleanOption(
        title = "Block Mixpanel",
        default = true,
        key = "blockMixpanel",
        description = "Product analytics platform",
    )
    val blockCleverTap by booleanOption(
        title = "Block CleverTap",
        default = true,
        key = "blockCleverTap",
        description = "User engagement and analytics",
    )
    val blockSegment by booleanOption(
        title = "Block Segment",
        default = true,
        key = "blockSegment",
        description = "Customer data pipeline and analytics",
    )
    val blockFacebook by booleanOption(
        title = "Block Facebook Analytics",
        default = true,
        key = "blockFacebook",
        description = "Meta's analytics and event logging",
    )
    val blockBranch by booleanOption(
        title = "Block Branch.io",
        default = true,
        key = "blockBranch",
        description = "Deep linking and attribution",
    )
    val blockUnity by booleanOption(
        title = "Block Unity Analytics",
        default = true,
        key = "blockUnity",
        description = "Unity engine analytics",
    )
    val blockFlurry by booleanOption(
        title = "Block Flurry",
        default = true,
        key = "blockFlurry",
        description = "Yahoo's analytics platform",
    )
    val blockGameAnalytics by booleanOption(
        title = "Block GameAnalytics",
        default = true,
        key = "blockGameAnalytics",
        description = "Game-focused analytics",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val detected = mutableListOf<String>()

        if (FirebaseInitializeFingerprint.methodOrNull != null) detected.add("Firebase")
        if (AppsFlyerStartFingerprint.methodOrNull != null) detected.add("AppsFlyer")
        if (AdjustOnCreateFingerprint.methodOrNull != null) detected.add("Adjust")
        if (BranchInitFingerprint.methodOrNull != null) detected.add("Branch")
        if (AmplitudeLogEventFingerprint.methodOrNull != null) detected.add("Amplitude")
        if (MixpanelTrackFingerprint.methodOrNull != null) detected.add("Mixpanel")
        if (CleverTapPushEventFingerprint.methodOrNull != null) detected.add("CleverTap")
        if (SegmentTrackFingerprint.methodOrNull != null) detected.add("Segment")
        if (FacebookLogEventFingerprint.methodOrNull != null) detected.add("Facebook Analytics")
        if (UnityAnalyticsTransactionFingerprint.methodOrNull != null) detected.add("Unity Analytics")
        if (FlurryLogEventFingerprint.methodOrNull != null) detected.add("Flurry")
        if (
            GameAnalyticsInitializeFingerprint.methodOrNull != null ||
            GameAnalyticsInitializeNoArgFingerprint.methodOrNull != null
        ) detected.add("GameAnalytics")

        if (detected.isEmpty()) {
            logger.warning("No supported telemetry SDKs found. No changes applied.")
            return@execute
        }

        detected.forEach { logger.info("Detected telemetry SDK: $it") }

        if (blockFirebase == true) {
            safeEarlyReturn(FirebaseInitializeFingerprint)
            safeEarlyReturn(FirebaseAnalyticsLogEventFingerprint)
            safeEarlyReturn(FirebaseCrashlyticsInitFingerprint)
        }

        if (blockAppsFlyer == true) {
            safeEarlyReturn(AppsFlyerStartFingerprint)
            safeEarlyReturn(AppsFlyerLogEventFingerprint)
        }

        if (blockAdjust == true) {
            safeEarlyReturn(AdjustOnCreateFingerprint)
            safeEarlyReturn(AdjustTrackEventFingerprint)
        }

        if (blockBranch == true) {
            safeEarlyReturn(BranchInitFingerprint)
        }

        if (blockAmplitude == true) {
            safeEarlyReturn(AmplitudeLogEventFingerprint)
        }
        if (blockMixpanel == true) {
            safeEarlyReturn(MixpanelTrackFingerprint)
        }
        if (blockCleverTap == true) {
            safeEarlyReturn(CleverTapPushEventFingerprint)
        }
        if (blockSegment == true) {
            safeEarlyReturn(SegmentTrackFingerprint)
        }
        if (blockFacebook == true) {
            safeEarlyReturn(FacebookLogEventFingerprint)
        }
        if (blockUnity == true) {
            safeEarlyReturn(UnityAnalyticsTransactionFingerprint)
        }
        if (blockFlurry == true) {
            safeEarlyReturn(FlurryLogEventFingerprint)
        }
        if (blockGameAnalytics == true) {
            safeEarlyReturn(GameAnalyticsInitializeFingerprint)
            safeEarlyReturn(GameAnalyticsInitializeNoArgFingerprint)
            safeEarlyReturn(GameAnalyticsDesignEventFingerprint)
        }

        val blocked = buildList {
            if (blockFirebase == true && FirebaseInitializeFingerprint.methodOrNull != null) add("Firebase")
            if (blockAppsFlyer == true && AppsFlyerStartFingerprint.methodOrNull != null) add("AppsFlyer")
            if (blockAdjust == true && AdjustOnCreateFingerprint.methodOrNull != null) add("Adjust")
            if (blockBranch == true && BranchInitFingerprint.methodOrNull != null) add("Branch")
            if (blockAmplitude == true && AmplitudeLogEventFingerprint.methodOrNull != null) add("Amplitude")
            if (blockMixpanel == true && MixpanelTrackFingerprint.methodOrNull != null) add("Mixpanel")
            if (blockCleverTap == true && CleverTapPushEventFingerprint.methodOrNull != null) add("CleverTap")
            if (blockSegment == true && SegmentTrackFingerprint.methodOrNull != null) add("Segment")
            if (blockFacebook == true && FacebookLogEventFingerprint.methodOrNull != null) add("Facebook")
            if (blockUnity == true && UnityAnalyticsTransactionFingerprint.methodOrNull != null) add("Unity")
            if (blockFlurry == true && FlurryLogEventFingerprint.methodOrNull != null) add("Flurry")
            if (blockGameAnalytics == true && (
                GameAnalyticsInitializeFingerprint.methodOrNull != null ||
                GameAnalyticsInitializeNoArgFingerprint.methodOrNull != null
            )) add("GameAnalytics")
        }
        blocked.forEach { logger.info("Blocked telemetry SDK: $it") }
        logger.info("Disable Telemetry patch succeeded (${blocked.size} SDK(s) blocked)")
    }
}
