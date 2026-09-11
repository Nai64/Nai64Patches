package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableRttPatch = bytecodePatch(
    name = "Disable RTT",
    description = "Makes TelephonyManager.isRttSupported() report false so apps that require RTT calling run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isRttSupported" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched RTT check(s) to false")
        else logger.warning("No RTT checks found. No changes applied.")
    }
}
