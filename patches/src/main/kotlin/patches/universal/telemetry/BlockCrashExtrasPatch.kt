package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val crashExtraStaticEntries = setOf(
    "init", "start", "launch", "register", "setup", "configure", "initialize",
    "send", "reportException", "logException",
)

private val crashExtraInstanceEntries = setOf(
    "send", "reportException", "logException", "logMessage", "start",
)

@Suppress("unused")
val blockCrashExtrasPatch = bytecodePatch(
    name = "Block Crash Extras",
    description = "Stops smaller crash reporters missed by Disable Crash Reporters so stack traces never upload",
    default = false,
) {
    category("Telemetry")
    val blockRaygun by booleanOption(
        title = "Block Raygun",
        default = true,
        key = "blockRaygun",
        description = "Crash reporting SDK.",
    )
    val blockShake by booleanOption(
        title = "Block Shake",
        default = true,
        key = "blockShake",
        description = "Bug reporting with screenshots SDK.",
    )
    val blockEmbrace by booleanOption(
        title = "Block Embrace",
        default = true,
        key = "blockEmbrace",
        description = "Mobile observability SDK.",
    )
    val blockSplunk by booleanOption(
        title = "Block Splunk Mint",
        default = true,
        key = "blockSplunkMint",
        description = "Legacy crash and event SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockRaygun == true) put("Lcom/mindscapehq", "Raygun")
            if (blockShake == true) put("Lcom/shakebugs", "Shake")
            if (blockEmbrace == true) put("Lio/embrace", "Embrace")
            if (blockSplunk == true) put("Lcom/splunk", "Splunk Mint")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, crashExtraStaticEntries, crashExtraInstanceEntries)
        if (hit.isEmpty()) logger.warning("No crash reporter SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
