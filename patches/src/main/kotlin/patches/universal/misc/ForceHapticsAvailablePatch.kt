package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceHapticsAvailablePatch = bytecodePatch(
    name = "Force Haptics Available",
    description = "Makes Vibrator.hasVibrator() report true so apps that require vibration/haptics run on devices without a vibrator.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/Vibrator;" to mapOf("hasVibrator" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched vibrator check(s) to true")
        else logger.warning("No vibrator checks found. No changes applied.")
    }
}
