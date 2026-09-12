package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val pushStaticEntries = setOf(
    "initWithContext", "startInit", "setAppId", "sendTag", "sendTags", "sendOutcome",
    "postNotification", "login", "logout", "takeOff", "configure", "init",
    "register", "setup", "setEmail", "setSMSNumber",
)

@Suppress("unused")
val blockPushAnalyticsPatch = bytecodePatch(
    name = "Block Push Analytics",
    description = "Stops push engagement SDKs so tags, outcomes and identity never reach push vendors",
    default = false,
) {
    category("Telemetry")
    val blockOneSignal by booleanOption(
        title = "Block OneSignal",
        default = true,
        key = "blockOneSignal",
        description = "Push notification and outcome SDK.",
    )
    val blockAirship by booleanOption(
        title = "Block Airship",
        default = true,
        key = "blockAirship",
        description = "Push engagement SDK, formerly Urban Airship.",
    )
    val blockBraze by booleanOption(
        title = "Block Braze",
        default = true,
        key = "blockBraze",
        description = "Messaging and engagement SDK, formerly Appboy.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockOneSignal == true) put("Lcom/onesignal", "OneSignal")
            if (blockAirship == true) put("Lcom/urbanairship", "Airship")
            if (blockBraze == true) {
                put("Lcom/braze", "Braze")
                put("Lcom/appboy", "Braze")
            }
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, pushStaticEntries)
        if (hit.isEmpty()) logger.warning("No push analytics SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
