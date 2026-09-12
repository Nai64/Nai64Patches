package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofMultiWindowModePatch = bytecodePatch(
    name = "Spoof Multi-Window Mode",
    description = "Makes multi-window checks report true so split-screen layouts stay active.",
    default = false,
) {
    category("Spoof")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/Activity;" to mapOf("isInMultiWindowMode" to "0x1")),
        )
        if (patched > 0) logger.info("Spoofed $patched multi-window check(s) to true")
        else logger.warning("No multi-window checks found. No changes applied.")
    }
}
