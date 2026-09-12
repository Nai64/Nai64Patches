package patches.universal.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.noOpVoidInvoke

@Suppress("unused")
val blockGmsTrackingPatch = bytecodePatch(
    name = "Block GMS Tracking",
    description = "Blocks Google activity recognition and geofencing so apps cannot track movement or location zones",
    default = false,
) {
    category("Privacy")
    val blockActivityRecognition by booleanOption(
        title = "Block activity recognition",
        default = true,
        key = "blockActivityRecognition",
        description = "Stop walking, driving and stillness detection requests.",
    )
    val blockGeofencing by booleanOption(
        title = "Block geofencing",
        default = true,
        key = "blockGeofencing",
        description = "Stop enter and exit tracking for location zones.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        if (blockActivityRecognition == true) {
            patched += noOpVoidInvoke(
                "Lcom/google/android/gms/location/ActivityRecognitionClient;",
                setOf("requestActivityUpdates", "removeActivityUpdates"),
            )
        }
        if (blockGeofencing == true) {
            patched += noOpVoidInvoke(
                "Lcom/google/android/gms/location/GeofencingClient;",
                setOf("addGeofences", "removeGeofences"),
            )
        }
        if (patched > 0) logger.info("Blocked GMS tracking at $patched call site(s)")
        else logger.warning("No GMS tracking calls found. No changes applied.")
    }
}
