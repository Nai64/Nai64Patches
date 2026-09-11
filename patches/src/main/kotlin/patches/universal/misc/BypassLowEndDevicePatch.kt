package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassLowEndDevicePatch = bytecodePatch(
    name = "Bypass Low-End Device",
    description = "Makes ActivityManager.isLowEndDevice() report false so apps stop applying low-end-device restrictions and degraded quality.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bypass") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/ActivityManager;" to mapOf("isLowEndDevice" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched low-end-device check(s) to false")
        else logger.warning("No low-end-device checks found. No changes applied.")
    }
}
