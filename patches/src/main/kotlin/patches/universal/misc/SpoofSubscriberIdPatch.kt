package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofSubscriberIdPatch = bytecodePatch(
    name = "Spoof Subscriber ID",
    description = "Reports a chosen subscriber id (IMSI) through TelephonyManager.getSubscriberId() so apps that fingerprint by IMSI see a constant value.",
    default = false,
) {
    val imsi by stringOption(
        title = "IMSI",
        default = "000000000000000",
        key = "subscriberId",
        description = "IMSI to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSubscriberId"),
            imsi ?: "000000000000000",
        )
        if (patched > 0) {
            logger.info("Spoofed subscriber id at $patched call site(s)")
        } else {
            logger.warning("No TelephonyManager.getSubscriberId call sites found. No changes applied.")
        }
    }
}
