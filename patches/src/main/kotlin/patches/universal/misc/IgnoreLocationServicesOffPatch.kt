package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreLocationServicesOffPatch = bytecodePatch(
    name = "Ignore Location Services Off",
    description =
        "Makes LocationManager.isLocationEnabled always return true so apps that refuse to " +
            "run when location services are disabled keep working",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/location/LocationManager;" to mapOf("isLocationEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched location-services check(s) to true")
        else logger.warning("No location-services checks found. No changes applied.")
    }
}
