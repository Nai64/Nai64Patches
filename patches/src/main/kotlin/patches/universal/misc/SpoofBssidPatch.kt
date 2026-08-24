package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBssidPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a constant value from WifiInfo.getBSSID() so apps cannot read the real access point MAC address.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getBSSID"),
            "02:00:00:00:00:00",
        )
        if (patched > 0) logger.info("Spoofed BSSID at $patched call site(s)")
        else logger.warning("No WifiInfo.getBSSID call sites found. No changes applied.")
    }
}
