package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val moEngageStaticEntries = setOf(
    "initialise", "initialize", "init", "start", "register", "setup",
)

private val moEngageInstanceEntries = setOf(
    "trackEvent", "setUserAttribute", "logout",
)

@Suppress("unused")
val blockMoEngagePatch = bytecodePatch(
    name = "Block MoEngage",
    description = "Stops the MoEngage SDK so events and profile traits never reach its engagement cloud",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/moengage" to "MoEngage"),
            moEngageStaticEntries,
            moEngageInstanceEntries,
        )
        if (hit.isEmpty()) logger.warning("No MoEngage SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
