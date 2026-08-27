package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeWifiScanResultsEmptyPatch = bytecodePatch(
    name = "Fake WiFi Scan Results Empty",
    description = "Makes WifiManager.getScanResults() return an empty list so apps cannot fingerprint nearby WiFi networks.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/net/wifi/WifiManager;",
            setOf("getScanResults"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) {
            logger.info("Faked WiFi scan results at $patched call site(s)")
        } else {
            logger.warning("No getScanResults call sites found. No changes applied.")
        }
    }
}
