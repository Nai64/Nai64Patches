package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableQuietModePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes UserManager.isQuietModeEnabled() report false so apps behave as if quiet mode is off.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isQuietModeEnabled" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched quiet-mode check(s) to false")
        else logger.warning("No quiet-mode checks found. No changes applied.")
    }
}
