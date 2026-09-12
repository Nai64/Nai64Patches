package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideServiceStatePatch = bytecodePatch(
    name = "Hide Service State",
    description = "Nulls cell service state so apps cannot read your voice and data registration or operator detail",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Landroid/telephony/TelephonyManager;",
            setOf("getServiceState"),
            "Landroid/telephony/ServiceState;",
        )
        if (patched > 0) logger.info("Hid service state at $patched call site(s)")
        else logger.warning("No service state calls found. No changes applied.")
    }
}
