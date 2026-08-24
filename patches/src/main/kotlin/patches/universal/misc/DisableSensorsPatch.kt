package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSensorsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes SensorManager.registerListener(...) report false (and register nothing) so the app cannot read motion, orientation, or other sensor data.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/hardware/SensorManager;" to mapOf("registerListener" to "0x0")),
        )
        if (patched > 0) logger.info("Disabled $patched sensor-registration call(s)")
        else logger.warning("No sensor-registration calls found. No changes applied.")
    }
}
