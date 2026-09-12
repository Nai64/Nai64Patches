package patches.universal.telemetry

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val huaweiStaticEntries = setOf("init", "setup", "register", "enableLog")

private val huaweiInstanceEntries = setOf(
    "onEvent", "setAnalyticsEnabled", "setUserId", "setUserProfile", "clearCachedData",
)

@Suppress("unused")
val blockHuaweiAnalyticsPatch = bytecodePatch(
    name = "Block Huawei Analytics",
    description = "Stops Huawei Analytics event uploads so usage never reaches Huawei servers",
    default = false,
) {
    category("Telemetry")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val hit = blockSdkMethods(
            mapOf("Lcom/huawei/hms/analytics" to "Huawei Analytics"),
            huaweiStaticEntries,
            huaweiInstanceEntries,
        )
        if (hit.isEmpty()) logger.warning("No Huawei Analytics SDK found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
