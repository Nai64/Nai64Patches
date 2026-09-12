package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val legacyAnalyticsEntries = setOf(
    "send", "activityStart", "activityStop", "reportActivityStart", "reportActivityStop",
    "dispatchLocalHits", "enableAutoActivityReports", "setDryRun",
)

@Suppress("unused")
val blockLegacyAnalyticsPatch = bytecodePatch(
    name = "Block Legacy Analytics",
    description = "Stops pre-Firebase Google Analytics so screen views and hits never upload",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf(
                "Lcom/google/analytics" to "Legacy Analytics",
                "Lcom/google/android/gms/analytics" to "Legacy Analytics",
            ),
            legacyAnalyticsEntries,
            legacyAnalyticsEntries,
        )
        if (hit.isEmpty()) logger.warning("No legacy analytics SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
