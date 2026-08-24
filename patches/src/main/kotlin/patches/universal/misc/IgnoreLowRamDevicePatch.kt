package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreLowRamDevicePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes ActivityManager.isLowRamDevice always return false so apps stop degrading " +
            "graphics or features on entry-level devices",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/app/ActivityManager;" to mapOf("isLowRamDevice" to "0x0")),
        )

        if (patched > 0) logger.info("Forced $patched low-RAM check(s) to false")
        else logger.warning("No low-RAM checks found. No changes applied.")
    }
}
