package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceDataCapablePatch = bytecodePatch(
    name = "Force Data Capable",
    description = "Makes TelephonyManager.isDataCapable() report true so apps that require mobile-data support run on Wi-Fi-only devices.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isDataCapable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched data-capable check(s) to true")
        else logger.warning("No data-capable checks found. No changes applied.")
    }
}
