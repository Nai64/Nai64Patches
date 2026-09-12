package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val tikTokStaticEntries = setOf(
    "initializeSdk", "init", "start", "trackEvent", "identify", "logout", "flush",
)

@Suppress("unused")
val blockTikTokEventsPatch = bytecodePatch(
    name = "Block TikTok Events",
    description = "Stops the TikTok Events SDK so app activity never reaches TikTok for ad targeting",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/tiktok" to "TikTok Events"),
            tikTokStaticEntries,
        )
        if (hit.isEmpty()) logger.warning("No TikTok Events SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
