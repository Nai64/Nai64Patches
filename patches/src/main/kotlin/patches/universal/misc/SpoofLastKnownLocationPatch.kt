package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofLastKnownLocationPatch = bytecodePatch(
    name = "Spoof Last Known Location",
    description =
        "Forces LocationManager.getLastKnownLocation() to return null so apps cannot " +
            "read the device's last cached GPS/network position.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldObjectGetterToNull(
            definingClass = "Landroid/location/LocationManager;",
            methodNames = setOf("getLastKnownLocation"),
            returnType = "Landroid/location/Location;",
        )

        if (patched > 0) {
            logger.info("Spoofed $patched last-known-location read(s)")
        } else {
            logger.warning("No last-known-location reads found. No changes applied.")
        }
    }
}
