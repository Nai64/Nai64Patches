package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceWifiP2pSupportedPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes WifiManager.isP2pSupported() report true so apps that need Wi-Fi Direct run on devices without P2P support.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/net/wifi/WifiManager;" to mapOf("isP2pSupported" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched wifi-p2p check(s) to true")
        else logger.warning("No wifi-p2p checks found. No changes applied.")
    }
}
