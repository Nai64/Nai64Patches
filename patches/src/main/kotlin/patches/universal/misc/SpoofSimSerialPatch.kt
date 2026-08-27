package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofSimSerialPatch = bytecodePatch(
    name = "Spoof SIM Serial Number",
    description = "Reports a chosen SIM serial number (ICCID) through TelephonyManager.getSimSerialNumber() so apps that fingerprint by ICCID see a constant value.",
    default = false,
) {
    val iccid by stringOption(
        title = "ICCID",
        default = "000000000000000",
        key = "simSerial",
        description = "ICCID to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSimSerialNumber"),
            iccid ?: "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed SIM serial at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getSimSerialNumber call sites found. No changes applied.")
        }
    }
}
