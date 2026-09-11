package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceScreenInteractivePatch = bytecodePatch(
    name = "Force Screen Interactive",
    description = "Makes PowerManager.isInteractive() report true so apps that pause or degrade when the screen is off keep running.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/PowerManager;" to mapOf("isInteractive" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched interactive check(s) to true")
        else logger.warning("No interactive checks found. No changes applied.")
    }
}
