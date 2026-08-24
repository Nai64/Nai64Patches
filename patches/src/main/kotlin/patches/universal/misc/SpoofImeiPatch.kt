package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Reports a fake IMEI through TelephonyManager.getDeviceId()/getImei() so apps
 * that fingerprint the device by IMEI see a constant value.
 */
@Suppress("unused")
val spoofImeiPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a fake IMEI through TelephonyManager.getDeviceId()/getImei() so apps that fingerprint the device by IMEI see a constant value.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getDeviceId", "getImei"),
            "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed IMEI at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getDeviceId/getImei call sites found. No changes applied.")
        }
    }
}
