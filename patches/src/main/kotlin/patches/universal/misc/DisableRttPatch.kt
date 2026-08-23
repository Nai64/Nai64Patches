package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableRttPatch = bytecodePatch(
    name = "Disable RTT",
    description = "Makes TelephonyManager.isRttSupported() report false so apps that require RTT calling run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isRttSupported" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched RTT check(s) to false")
        else logger.warning("No RTT checks found. No changes applied.")
    }
}
