package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofBssidPatch = bytecodePatch(
    name = "Spoof BSSID",
    description = "Reports a chosen value from WifiInfo.getBSSID() so apps cannot read the real access point MAC address.",
    default = false,
) {
    val bssid by stringOption(
        title = "BSSID",
        default = "02:00:00:00:00:00",
        key = "bssid",
        description = "BSSID to report (e.g. 02:00:00:00:00:00).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getBSSID"),
            bssid ?: "02:00:00:00:00:00",
        )
        if (patched > 0) logger.info("Spoofed BSSID at $patched call site(s)")
        else logger.warning("No WifiInfo.getBSSID call sites found. No changes applied.")
    }
}
