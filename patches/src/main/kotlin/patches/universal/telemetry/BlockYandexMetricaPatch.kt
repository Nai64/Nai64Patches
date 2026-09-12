package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val metricaStaticEntries = setOf(
    "activate", "activateReporter", "reportEvent", "reportError", "reportRevenue",
    "resumeSession", "pauseSession", "sendEventsBuffer", "register",
)

private val metricaInstanceEntries = setOf(
    "reportEvent", "reportError", "resumeSession", "pauseSession", "sendEventsBuffer",
)

@Suppress("unused")
val blockYandexMetricaPatch = bytecodePatch(
    name = "Block Yandex Metrica",
    description = "Stops Yandex Metrica activation and event reports so usage never reaches Yandex servers",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/yandex/metrica" to "Yandex Metrica"),
            metricaStaticEntries,
            metricaInstanceEntries,
        )
        if (hit.isEmpty()) logger.warning("No Yandex Metrica SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
