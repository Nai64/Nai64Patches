package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideDefaultSensorPatch = bytecodePatch(
    name = "Hide Default Sensor",
    description = "Makes SensorManager.getDefaultSensor return null so sensor fingerprint and VR gates see no hardware",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldObjectGetterToNull(
            definingClass = "Landroid/hardware/SensorManager;",
            methodNames = setOf("getDefaultSensor"),
            returnType = "Landroid/hardware/Sensor;",
        )
        if (patched > 0) logger.info("Hid default sensor at $patched call site(s)")
        else logger.warning("No getDefaultSensor calls found. No changes applied.")
    }
}
