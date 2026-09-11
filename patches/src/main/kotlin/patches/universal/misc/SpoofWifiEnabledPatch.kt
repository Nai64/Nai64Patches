package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofWifiEnabledPatch = bytecodePatch(
    name = "Spoof WiFi Enabled",
    description =
        "Forces WifiManager.isWifiEnabled() to report true and getWifiState() to report " +
            "WIFI_STATE_ENABLED (3) so apps cannot tell Wi-Fi is off.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val enabled = foldBooleanGetterConst(
            definingClass = "Landroid/net/wifi/WifiManager;",
            methodNames = setOf("isWifiEnabled"),
            value = true,
        )
        val state = foldNoArgIntGetter(
            definingClass = "Landroid/net/wifi/WifiManager;",
            methodNames = setOf("getWifiState"),
            value = 3, // WifiManager.WIFI_STATE_ENABLED
        )

        val total = enabled + state
        if (total > 0) {
            logger.info("Spoofed $total Wi-Fi read(s)")
        } else {
            logger.warning("No Wi-Fi reads found. No changes applied.")
        }
    }
}
