package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val attributionStaticEntries = setOf(
    "init", "initialize", "configure", "start", "register", "setup",
)

private val attributionInstanceEntries = setOf(
    "connect", "eventWithName", "track", "trackEvent", "logEvent", "sendEvent",
)

@Suppress("unused")
val blockAttributionExtrasPatch = bytecodePatch(
    name = "Block Attribution Extras",
    description = "Stops smaller attribution SDKs so installs and opens are never credited to ad networks",
    default = false,
) {
    category("Telemetry")
    val blockKochava by booleanOption(
        title = "Block Kochava",
        default = true,
        key = "blockKochava",
        description = "Mobile attribution and analytics SDK.",
    )
    val blockSingular by booleanOption(
        title = "Block Singular",
        default = true,
        key = "blockSingular",
        description = "Marketing attribution SDK.",
    )
    val blockTenjin by booleanOption(
        title = "Block Tenjin",
        default = true,
        key = "blockTenjin",
        description = "Install attribution SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockKochava == true) put("Lcom/kochava", "Kochava")
            if (blockSingular == true) put("Lcom/singular", "Singular")
            if (blockTenjin == true) put("Lcom/tenjin", "Tenjin")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, attributionStaticEntries, attributionInstanceEntries)
        if (hit.isEmpty()) logger.warning("No attribution SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
