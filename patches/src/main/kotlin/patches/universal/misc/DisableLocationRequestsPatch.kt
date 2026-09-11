package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableLocationRequestsPatch = bytecodePatch(
    name = "Disable Location Requests",
    description = "Stops the app from tracking your location.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
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
