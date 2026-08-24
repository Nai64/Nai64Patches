package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val force5GhzBandSupportedPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes WifiManager.is5GHzBandSupported() report true so apps that require 5GHz Wi-Fi functionality run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/net/wifi/WifiManager;" to mapOf("is5GHzBandSupported" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched 5GHz-band check(s) to true")
        else logger.warning("No 5GHz-band checks found. No changes applied.")
    }
}
