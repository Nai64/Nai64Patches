package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val force5GhzBandSupportedPatch = bytecodePatch(
    name = "Force 5GHz Band Supported",
    description = "Makes WifiManager.is5GHzBandSupported() report true so apps that require 5GHz Wi-Fi functionality run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/net/wifi/WifiManager;" to mapOf("is5GHzBandSupported" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched 5GHz-band check(s) to true")
        else logger.warning("No 5GHz-band checks found. No changes applied.")
    }
}
