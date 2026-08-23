package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceWifiScanAlwaysAvailablePatch = bytecodePatch(
    name = "Force WiFi Scan Always Available",
    description = "Makes WifiManager.isScanAlwaysAvailable() report true so apps that need background scanning see it as enabled.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/net/wifi/WifiManager;" to mapOf("isScanAlwaysAvailable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched wifi-scan check(s) to true")
        else logger.warning("No wifi-scan checks found. No changes applied.")
    }
}
