package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofWifiRssiPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a fixed signal strength (-50 dBm) from WifiInfo.getRssi() so apps cannot read the real Wi-Fi signal level.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getRssi"),
            -50,
        )
        if (patched > 0) logger.info("Spoofed Wi-Fi RSSI at $patched call site(s)")
        else logger.warning("No getRssi call sites found. No changes applied.")
    }
}
