package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val replayStaticEntries = setOf(
    "init", "start", "startWithKey", "startWithConfiguration", "startNewSession",
    "setup", "setupAndStartRecording", "register", "initialize", "initializeSdk",
    "begin", "launch", "configure",
)

private val replayInstanceEntries = setOf(
    "startRecording", "startNewSession", "startSession", "event", "identify",
)

@Suppress("unused")
val blockSessionReplayPatch = bytecodePatch(
    name = "Block Session Replay",
    description = "Stops session recording SDKs so apps cannot film your taps, scrolling or screen content",
    default = false,
) {
    category("Telemetry")
    val blockUxcam by booleanOption(
        title = "Block UXCam",
        default = true,
        key = "blockUxcam",
        description = "Session replay and heatmap SDK.",
    )
    val blockSmartlook by booleanOption(
        title = "Block Smartlook",
        default = true,
        key = "blockSmartlook",
        description = "Session recording and funnel SDK.",
    )
    val blockFullStory by booleanOption(
        title = "Block FullStory",
        default = true,
        key = "blockFullStory",
        description = "Digital experience recording SDK.",
    )
    val blockContentsquare by booleanOption(
        title = "Block Contentsquare",
        default = true,
        key = "blockContentsquare",
        description = "Experience analytics and zoning SDK.",
    )
    val blockTestFairy by booleanOption(
        title = "Block TestFairy",
        default = true,
        key = "blockTestFairy",
        description = "Beta session video recording SDK.",
    )
    val blockBugsee by booleanOption(
        title = "Block Bugsee",
        default = true,
        key = "blockBugsee",
        description = "Bug video report recording SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockUxcam == true) put("Lcom/uxcam", "UXCam")
            if (blockSmartlook == true) put("Lcom/smartlook", "Smartlook")
            if (blockFullStory == true) put("Lcom/fullstory", "FullStory")
            if (blockContentsquare == true) put("Lcom/contentsquare", "Contentsquare")
            if (blockTestFairy == true) put("Lcom/testfairy", "TestFairy")
            if (blockBugsee == true) put("Lcom/bugsee", "Bugsee")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, replayStaticEntries, replayInstanceEntries)
        if (hit.isEmpty()) logger.warning("No session replay SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
