package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofMacAddressPatch = bytecodePatch(
    name = "Spoof MAC Address",
    description = "Reports a constant MAC address through WifiInfo.getMacAddress() so apps cannot fingerprint the device by its Wi-Fi MAC.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/net/wifi/WifiInfo;",
            setOf("getMacAddress"),
            "02:00:00:00:00:00",
        )
        if (patched > 0) logger.info("Spoofed MAC address at $patched call site(s)")
        else logger.warning("No WifiInfo.getMacAddress call sites found. No changes applied.")
    }
}
