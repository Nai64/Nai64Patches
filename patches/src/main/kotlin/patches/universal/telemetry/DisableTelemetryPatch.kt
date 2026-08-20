package patches.universal.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

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
        description = "",
    )
    val blockAppsFlyer by booleanOption(
        title = "Block AppsFlyer",
        default = true,
        key = "blockAppsFlyer",
        description = "",
    )
    val blockAdjust by booleanOption(
        title = "Block Adjust",
        default = true,
        key = "blockAdjust",
        description = "",
    )
    val blockAmplitude by booleanOption(
        title = "Block Amplitude",
        default = true,
        key = "blockAmplitude",
        description = "",
    )
    val blockMixpanel by booleanOption(
        title = "Block Mixpanel",
        default = true,
        key = "blockMixpanel",
        description = "",
    )
    val blockCleverTap by booleanOption(
        title = "Block CleverTap",
        default = true,
        key = "blockCleverTap",
        description = "",
    )
    val blockSegment by booleanOption(
        title = "Block Segment",
        default = true,
        key = "blockSegment",
        description = "",
    )
    val blockFacebook by booleanOption(
        title = "Block Facebook Analytics",
        default = true,
        key = "blockFacebook",
        description = "",
    )
    val blockBranch by booleanOption(
        title = "Block Branch.io",
        default = true,
        key = "blockBranch",
        description = "",
    )
    val blockUnity by booleanOption(
        title = "Block Unity Analytics",
        default = true,
        key = "blockUnity",
        description = "",
    )
    val blockFlurry by booleanOption(
        title = "Block Flurry",
        default = true,
        key = "blockFlurry",
        description = "",
    )
    val blockGameAnalytics by booleanOption(
        title = "Block GameAnalytics",
        default = true,
        key = "blockGameAnalytics",
        description = "",
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
            FirebaseInitializeFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            FirebaseAnalyticsLogEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            FirebaseCrashlyticsInitFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        if (blockAppsFlyer == true) {
            AppsFlyerStartFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AppsFlyerLogEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        if (blockAdjust == true) {
            AdjustOnCreateFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            AdjustTrackEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        if (blockBranch == true) {
            BranchInitFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }

        if (blockAmplitude == true) {
            AmplitudeLogEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockMixpanel == true) {
            MixpanelTrackFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockCleverTap == true) {
            CleverTapPushEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockSegment == true) {
            SegmentTrackFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockFacebook == true) {
            FacebookLogEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockUnity == true) {
            UnityAnalyticsTransactionFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockFlurry == true) {
            FlurryLogEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
        }
        if (blockGameAnalytics == true) {
            GameAnalyticsInitializeFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            GameAnalyticsInitializeNoArgFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
            GameAnalyticsDesignEventFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void") }
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
            if (blockGameAnalytics == true && GameAnalyticsInitializeFingerprint.methodOrNull != null) add("GameAnalytics")
        }
        blocked.forEach { logger.info("Blocked telemetry SDK: $it") }
        logger.info("Disable Telemetry patch succeeded (${blocked.size} SDK(s) blocked)")
    }
}
