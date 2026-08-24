package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceSingleSimPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes TelephonyManager.getSimCount() report 1 so apps that require or count multiple SIMs run on a single-SIM device.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSimCount"),
            1,
        )
        if (patched > 0) logger.info("Forced $patched sim-count read(s) to 1")
        else logger.warning("No getSimCount call sites found. No changes applied.")
    }
}
