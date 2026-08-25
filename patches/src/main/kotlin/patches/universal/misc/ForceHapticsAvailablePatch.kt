package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceHapticsAvailablePatch = bytecodePatch(
    name = "Force Haptics Available",
    description = "Makes Vibrator.hasVibrator() report true so apps that require vibration/haptics run on devices without a vibrator.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/Vibrator;" to mapOf("hasVibrator" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched vibrator check(s) to true")
        else logger.warning("No vibrator checks found. No changes applied.")
    }
}
