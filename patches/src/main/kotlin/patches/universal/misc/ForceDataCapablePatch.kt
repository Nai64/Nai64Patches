package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceDataCapablePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes TelephonyManager.isDataCapable() report true so apps that require mobile-data support run on Wi-Fi-only devices.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isDataCapable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched data-capable check(s) to true")
        else logger.warning("No data-capable checks found. No changes applied.")
    }
}
