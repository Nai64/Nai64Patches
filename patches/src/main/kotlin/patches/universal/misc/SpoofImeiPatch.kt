package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofImeiPatch = bytecodePatch(
    name = "Spoof IMEI",
    description = "Fakes the device IMEI.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
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
