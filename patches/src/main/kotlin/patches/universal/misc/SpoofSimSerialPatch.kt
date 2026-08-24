package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Reports a fake SIM serial number (ICCID) through
 * TelephonyManager.getSimSerialNumber() so apps that fingerprint by ICCID see a
 * constant value.
 */
@Suppress("unused")
val spoofSimSerialPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a fake SIM serial number through TelephonyManager.getSimSerialNumber() so apps that fingerprint by ICCID see a constant value.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSimSerialNumber"),
            "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed SIM serial at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getSimSerialNumber call sites found. No changes applied.")
        }
    }
}
