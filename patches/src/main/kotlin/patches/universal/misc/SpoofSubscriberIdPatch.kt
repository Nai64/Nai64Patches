package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Reports a fake subscriber id (IMSI) through TelephonyManager.getSubscriberId()
 * so apps that fingerprint by IMSI see a constant value.
 */
@Suppress("unused")
val spoofSubscriberIdPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a fake subscriber id (IMSI) through TelephonyManager.getSubscriberId() so apps that fingerprint by IMSI see a constant value.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSubscriberId"),
            "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed subscriber id at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getSubscriberId call sites found. No changes applied.")
        }
    }
}
