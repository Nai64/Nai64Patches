package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideDataUsagePatch = bytecodePatch(
    name = "Hide Data Usage",
    description = "Nulls network stats queries so apps cannot read your mobile and WiFi data consumption",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldObjectGetterToNull(
            "Landroid/app/usage/NetworkStatsManager;",
            setOf("querySummary", "queryDetails"),
            "Landroid/app/usage/NetworkStats;",
        )
        patched += foldObjectGetterToNull(
            "Landroid/app/usage/NetworkStatsManager;",
            setOf("querySummaryForDevice"),
            "Landroid/app/usage/NetworkStats\$Bucket;",
        )
        if (patched > 0) logger.info("Hid data usage at $patched call site(s)")
        else logger.warning("No data usage calls found. No changes applied.")
    }
}
