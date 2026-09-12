package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val postHogStaticEntries = setOf(
    "init", "capture", "identify", "screen", "alias", "flush", "reset",
    "optOut", "optIn", "close", "setup", "register", "start",
)

@Suppress("unused")
val blockPostHogPatch = bytecodePatch(
    name = "Block PostHog",
    description = "Stops the PostHog SDK so captures, screens and feature flags never upload",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/posthog" to "PostHog"),
            postHogStaticEntries,
        )
        if (hit.isEmpty()) logger.warning("No PostHog SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
