package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import java.util.logging.Logger

@Suppress("unused")
val spoofWifiRssiPatch = bytecodePatch(
    name = "Spoof WiFi RSSI",
    description = "Reports a chosen signal strength from WifiInfo.getRssi() so apps cannot read the real Wi-Fi signal level.",
    default = false,
) {
    val rssi by intOption(
        title = "RSSI (dBm)",
        default = -50,
        key = "wifiRssi",
        description = "RSSI in dBm (-30 strong, -50 moderate, -80 weak).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getRssi"),
            rssi ?: -50,
        )
        if (patched > 0) logger.info("Spoofed Wi-Fi RSSI at $patched call site(s)")
        else logger.warning("No getRssi call sites found. No changes applied.")
    }
}
