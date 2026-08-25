package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofWifiSsidPatch = bytecodePatch(
    name = "Spoof WiFi SSID",
    description = "Reports a constant value from WifiInfo.getSSID() so apps cannot read the real connected network name.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getSSID"),
            "unknown",
        )
        if (patched > 0) logger.info("Spoofed WiFi SSID at $patched call site(s)")
        else logger.warning("No WifiInfo.getSSID call sites found. No changes applied.")
    }
}
