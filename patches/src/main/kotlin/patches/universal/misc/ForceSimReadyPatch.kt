package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceSimReadyPatch = bytecodePatch(
    name = "Force SIM Ready",
    description = "Makes TelephonyManager.getSimState() report SIM_STATE_READY (5) so apps that wait for a ready SIM run immediately.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSimState"),
            5,
        )
        if (patched > 0) logger.info("Forced $patched SIM-state read(s) to READY")
        else logger.warning("No getSimState call sites found. No changes applied.")
    }
}
