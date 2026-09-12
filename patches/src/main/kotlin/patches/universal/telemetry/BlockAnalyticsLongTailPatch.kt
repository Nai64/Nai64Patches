package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val longTailStaticEntries = setOf(
    "init", "start", "integrate", "register", "setup", "configure", "initialize",
)

private val longTailInstanceEntries = setOf(
    "track", "trackEvent", "tagEvent", "tagScreen", "recordEvent", "dispatch",
    "setUserId", "setUserAttributes", "openSession", "closeSession", "upload",
)

@Suppress("unused")
val blockAnalyticsLongTailPatch = bytecodePatch(
    name = "Block Analytics Long Tail",
    description = "Stops smaller product analytics SDKs so events never reach their dashboards",
    default = false,
) {
    category("Telemetry")
    val blockMatomo by booleanOption(
        title = "Block Matomo",
        default = true,
        key = "blockMatomo",
        description = "Open source product analytics SDK.",
    )
    val blockLeanplum by booleanOption(
        title = "Block Leanplum",
        default = true,
        key = "blockLeanplum",
        description = "Lifecycle messaging and analytics SDK.",
    )
    val blockLocalytics by booleanOption(
        title = "Block Localytics",
        default = true,
        key = "blockLocalytics",
        description = "App analytics and marketing SDK.",
    )
    val blockWebEngage by booleanOption(
        title = "Block WebEngage",
        default = true,
        key = "blockWebEngage",
        description = "Retention and journey analytics SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockMatomo == true) put("Lorg/matomo", "Matomo")
            if (blockLeanplum == true) put("Lcom/leanplum", "Leanplum")
            if (blockLocalytics == true) put("Lcom/localytics", "Localytics")
            if (blockWebEngage == true) put("Lcom/webengage", "WebEngage")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, longTailStaticEntries, longTailInstanceEntries)
        if (hit.isEmpty()) logger.warning("No long tail analytics SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
