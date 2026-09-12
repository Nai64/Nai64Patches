package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val pipelineStaticEntries = setOf(
    "start", "init", "initializeSharedInstance", "register", "setup",
)

private val pipelineInstanceEntries = setOf(
    "logEvent", "logScreenEvent", "logError", "logException", "logNetworkPerformance",
    "addEvent", "uploadEvents",
)

@Suppress("unused")
val blockDataPipelinesPatch = bytecodePatch(
    name = "Block Data Pipelines",
    description = "Stops customer data pipeline SDKs so events never stream into warehouses or profiles",
    default = false,
) {
    category("Telemetry")
    val blockMparticle by booleanOption(
        title = "Block mParticle",
        default = true,
        key = "blockMparticle",
        description = "Customer data platform SDK.",
    )
    val blockTreasureData by booleanOption(
        title = "Block Treasure Data",
        default = true,
        key = "blockTreasureData",
        description = "Cloud data pipeline SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockMparticle == true) put("Lcom/mparticle", "mParticle")
            if (blockTreasureData == true) put("Lcom/treasuredata", "Treasure Data")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, pipelineStaticEntries, pipelineInstanceEntries)
        if (hit.isEmpty()) logger.warning("No data pipeline SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
