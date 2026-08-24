package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceIdleCallStatePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes TelephonyManager.getCallState() report CALL_STATE_IDLE (0) so apps that react to active calls behave as if none are in progress.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getCallState"),
            0,
        )
        if (patched > 0) logger.info("Forced $patched call-state read(s) to IDLE")
        else logger.warning("No getCallState call sites found. No changes applied.")
    }
}
