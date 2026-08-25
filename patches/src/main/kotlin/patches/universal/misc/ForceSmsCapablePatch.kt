package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceSmsCapablePatch = bytecodePatch(
    name = "Force SMS Capable",
    description = "Makes TelephonyManager.isSmsCapable() report true so apps that require SMS support run on devices without a cellular radio.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isSmsCapable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched SMS-capable check(s) to true")
        else logger.warning("No SMS-capable checks found. No changes applied.")
    }
}
