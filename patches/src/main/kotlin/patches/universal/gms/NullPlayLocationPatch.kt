package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object FusedLastLocationFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/location/FusedLocationProviderClient;",
    name = "getLastLocation",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

internal object FusedRequestUpdatesFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/location/FusedLocationProviderClient;",
    name = "requestLocationUpdates",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

internal object FusedFlushLocationsFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/location/FusedLocationProviderClient;",
    name = "flushLocations",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

@Suppress("unused")
val nullPlayLocationPatch = bytecodePatch(
    name = "Null Play Location",
    description = "Makes Play Services location return empty results: last location resolves to null and update requests complete instantly without fixes. Apps fall back to no-location behavior.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsTaskNullAll(logger, FusedLastLocationFingerprint, "Null Play Location: getLastLocation")
        total += gmsTaskNullAll(logger, FusedRequestUpdatesFingerprint, "Null Play Location: requestLocationUpdates")
        total += gmsTaskNullAll(logger, FusedFlushLocationsFingerprint, "Null Play Location: flushLocations")
        if (total == 0) logger.warning("Null Play Location: no FusedLocationProviderClient methods found. No changes applied.")
        else logger.info("Null Play Location: nulled $total method(s) in total")
    }
}
