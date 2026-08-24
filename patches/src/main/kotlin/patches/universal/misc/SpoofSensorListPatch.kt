package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofSensorListPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Forces SensorManager.getSensorList() to return an empty list so apps cannot " +
            "enumerate the device's sensors for fingerprinting or capability checks.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = replaceGetterWithStaticCall(
            definingClass = "Landroid/hardware/SensorManager;",
            methodNames = setOf("getSensorList"),
            returnType = "Ljava/util/List;",
            replacementInvoke =
                "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )

        if (patched > 0) {
            logger.info("Spoofed $patched sensor-list read(s)")
        } else {
            logger.warning("No sensor-list reads found. No changes applied.")
        }
    }
}
