package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceAppActivePatch = bytecodePatch(
    name = "Force App Active",
    description = "Makes UsageStatsManager.isAppInactive() report false so apps are not throttled by Android's app-standby buckets.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/usage/UsageStatsManager;" to mapOf("isAppInactive" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched app-inactive check(s) to false")
        else logger.warning("No app-inactive checks found. No changes applied.")
    }
}
