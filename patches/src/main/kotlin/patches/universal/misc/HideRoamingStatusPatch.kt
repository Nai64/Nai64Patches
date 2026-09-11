package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideRoamingStatusPatch = bytecodePatch(
    name = "Hide Roaming Status",
    description =
        "Makes TelephonyManager.isNetworkRoaming always return false so apps that block or " +
            "warn while roaming behave as if the device is on its home network",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isNetworkRoaming" to "0x0")),
        )

        if (patched > 0) logger.info("Hid $patched roaming check(s)")
        else logger.warning("No roaming checks found. No changes applied.")
    }
}
