package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSensorsPatch = bytecodePatch(
    name = "Disable Sensors",
    description = "Stops the app from reading motion and orientation sensors.",
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
