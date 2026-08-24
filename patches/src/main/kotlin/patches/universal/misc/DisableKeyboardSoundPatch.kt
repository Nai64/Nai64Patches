package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableKeyboardSoundPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops AudioManager.playSoundEffect(...) so keyboard/typing and other system sound effects are silenced inside the patched app (typically an IME).",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/media/AudioManager;", setOf("playSoundEffect"))
        if (patched > 0) logger.info("Silenced $patched playSoundEffect() call(s)")
        else logger.warning("No playSoundEffect() calls found. No changes applied.")
    }
}
