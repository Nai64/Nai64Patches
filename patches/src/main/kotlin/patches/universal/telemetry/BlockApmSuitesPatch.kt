package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val apmStaticEntries = setOf(
    "withApplicationToken", "startup", "start", "init", "initialize", "register", "setup",
    "recordMetric", "recordCustomMetric", "recordHandledException", "recordBreadcrumb",
    "noticeHttpTransaction", "noticeNetworkFailure", "applyUserPrivacyOptions",
)

@Suppress("unused")
val blockApmSuitesPatch = bytecodePatch(
    name = "Block APM Suites",
    description = "Stops app performance monitoring SDKs so timings, crashes and network traces never upload",
    default = false,
) {
    category("Telemetry")
    val blockNewRelic by booleanOption(
        title = "Block New Relic",
        default = true,
        key = "blockNewRelic",
        description = "Mobile performance monitoring SDK.",
    )
    val blockDatadog by booleanOption(
        title = "Block Datadog",
        default = true,
        key = "blockDatadog",
        description = "Observability and real user monitoring SDK.",
    )
    val blockDynatrace by booleanOption(
        title = "Block Dynatrace",
        default = true,
        key = "blockDynatrace",
        description = "Performance and experience monitoring SDK.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockNewRelic == true) put("Lcom/newrelic", "New Relic")
            if (blockDatadog == true) put("Lcom/datadog", "Datadog")
            if (blockDynatrace == true) put("Lcom/dynatrace", "Dynatrace")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, apmStaticEntries)
        if (hit.isEmpty()) logger.warning("No APM SDKs found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
