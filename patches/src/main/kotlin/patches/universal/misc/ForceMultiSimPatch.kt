package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceMultiSimPatch = bytecodePatch(
    name = "Force Multi-SIM",
    description = "Makes TelephonyManager.isMultiSimEnabled() report true so apps that require multiple SIMs run on single-SIM devices.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isMultiSimEnabled" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched multi-sim check(s) to true")
        else logger.warning("No multi-sim checks found. No changes applied.")
    }
}
