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
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
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
