package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableQuietModePatch = bytecodePatch(
    name = "Disable Quiet Mode",
    description = "Makes UserManager.isQuietModeEnabled() report false so apps behave as if quiet mode is off.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isQuietModeEnabled" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched quiet-mode check(s) to false")
        else logger.warning("No quiet-mode checks found. No changes applied.")
    }
}
