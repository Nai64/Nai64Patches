package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHapticFeedbackPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes View.performHapticFeedback(...) report false (and do nothing) so the app's haptic/vibration feedback is suppressed.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/view/View;" to mapOf("performHapticFeedback" to "0x0")),
        )
        if (patched > 0) logger.info("Disabled $patched haptic-feedback call(s)")
        else logger.warning("No haptic-feedback calls found. No changes applied.")
    }
}
