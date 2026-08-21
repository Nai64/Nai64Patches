package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreGpsDisabledPatch = bytecodePatch(
    name = "Ignore GPS Disabled",
    description =
        "Makes LocationManager.isProviderEnabled always return true so apps that refuse to " +
            "run without location services keep working",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/location/LocationManager;" to mapOf("isProviderEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched provider check(s) to enabled")
        else logger.warning("No provider checks found. No changes applied.")
    }
}
