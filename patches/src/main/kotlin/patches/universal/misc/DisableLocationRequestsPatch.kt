package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableLocationRequestsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops LocationManager.requestLocationUpdates/requestSingleUpdate and FusedLocationProviderClient.requestLocationUpdates so the app receives no location updates at all.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Landroid/location/LocationManager;",
            setOf("requestLocationUpdates", "requestSingleUpdate"),
        ) + noOpVoidInvoke(
            "Lcom/google/android/gms/location/FusedLocationProviderClient;",
            setOf("requestLocationUpdates"),
        )
        if (patched > 0) logger.info("Neutralized $patched location-request call(s)")
        else logger.warning("No location-request calls found. No changes applied.")
    }
}
