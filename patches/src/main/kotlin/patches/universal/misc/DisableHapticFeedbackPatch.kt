package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHapticFeedbackPatch = bytecodePatch(
    name = "Disable Haptic Feedback",
    description = "Disables vibration feedback.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/view/View;" to mapOf("performHapticFeedback" to "0x0")),
        )
        if (patched > 0) logger.info("Disabled $patched haptic-feedback call(s)")
        else logger.warning("No haptic-feedback calls found. No changes applied.")
    }
}
