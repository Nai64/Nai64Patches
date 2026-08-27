package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofWifiSsidPatch = bytecodePatch(
    name = "Spoof WiFi SSID",
    description = "Reports a chosen value from WifiInfo.getSSID() so apps cannot read the real connected network name.",
    default = false,
) {
    val ssid by stringOption(
        title = "WiFi SSID",
        default = "unknown",
        key = "wifiSsid",
        description = "SSID to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getSSID"),
            ssid ?: "unknown",
        )
        if (patched > 0) logger.info("Spoofed WiFi SSID at $patched call site(s)")
        else logger.warning("No WifiInfo.getSSID call sites found. No changes applied.")
    }
}
