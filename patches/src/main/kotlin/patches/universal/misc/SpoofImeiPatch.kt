package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofImeiPatch = bytecodePatch(
    name = "Spoof IMEI",
    description = "Reports a chosen IMEI through TelephonyManager.getDeviceId()/getImei() so apps that fingerprint the device by IMEI see a constant value.",
    default = false,
) {
    val imei by stringOption(
        title = "IMEI",
        default = "000000000000000",
        key = "imei",
        description = "IMEI to report (15 digits).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getDeviceId", "getImei"),
            imei ?: "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed IMEI at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getDeviceId/getImei call sites found. No changes applied.")
        }
    }
}
