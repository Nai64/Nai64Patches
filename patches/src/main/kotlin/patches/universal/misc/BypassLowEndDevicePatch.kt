package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassLowEndDevicePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes ActivityManager.isLowEndDevice() report false so apps stop applying low-end-device restrictions and degraded quality.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/ActivityManager;" to mapOf("isLowEndDevice" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched low-end-device check(s) to false")
        else logger.warning("No low-end-device checks found. No changes applied.")
    }
}
