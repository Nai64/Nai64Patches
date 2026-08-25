package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceWorldPhonePatch = bytecodePatch(
    name = "Force World Phone",
    description = "Makes TelephonyManager.isWorldPhone() report true so apps that gate features to world phones run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isWorldPhone" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched world-phone check(s) to true")
        else logger.warning("No world-phone checks found. No changes applied.")
    }
}
