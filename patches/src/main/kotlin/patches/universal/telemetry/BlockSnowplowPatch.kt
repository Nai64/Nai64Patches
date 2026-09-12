package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val snowplowStaticEntries = setOf(
    "init", "createTracker", "register", "setup", "start",
)

private val snowplowInstanceEntries = setOf(
    "track", "addContext", "setUserId", "setSubject",
)

@Suppress("unused")
val blockSnowplowPatch = bytecodePatch(
    name = "Block Snowplow",
    description = "Stops the Snowplow tracker so structured events never reach its collectors",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/snowplowanalytics" to "Snowplow"),
            snowplowStaticEntries,
            snowplowInstanceEntries,
        )
        if (hit.isEmpty()) logger.warning("No Snowplow SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
