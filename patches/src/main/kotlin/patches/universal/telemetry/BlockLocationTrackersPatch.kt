package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val locationTrackerStaticEntries = setOf(
    "initialize", "init", "start", "register", "setup", "configure",
)

private val locationTrackerInstanceEntries = setOf(
    "start", "startTracking", "track", "sendLocation", "onLocationChanged",
)

@Suppress("unused")
val blockLocationTrackersPatch = bytecodePatch(
    name = "Block Location Trackers",
    description = "Stops location intelligence SDKs so visits and beacon sightings never upload",
    default = false,
) {
    category("Telemetry")
    val blockRadar by booleanOption(
        title = "Block Radar",
        default = true,
        key = "blockRadar",
        description = "Geofencing and place visit SDK.",
    )
    val blockGimbal by booleanOption(
        title = "Block Gimbal",
        default = true,
        key = "blockGimbal",
        description = "Beacon proximity SDK.",
    )
    val blockEstimote by booleanOption(
        title = "Block Estimote",
        default = true,
        key = "blockEstimote",
        description = "Beacon proximity SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockRadar == true) put("Lio/radar", "Radar")
            if (blockGimbal == true) put("Lcom/gimbal", "Gimbal")
            if (blockEstimote == true) put("Lcom/estimote", "Estimote")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, locationTrackerStaticEntries, locationTrackerInstanceEntries)
        if (hit.isEmpty()) logger.warning("No location tracker SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
