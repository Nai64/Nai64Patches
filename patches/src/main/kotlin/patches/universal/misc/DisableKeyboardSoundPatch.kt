package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableKeyboardSoundPatch = bytecodePatch(
    name = "Disable Keyboard Sound",
    description = "Silences keyboard and system sounds.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/media/AudioManager;", setOf("playSoundEffect"))
        if (patched > 0) logger.info("Silenced $patched playSoundEffect() call(s)")
        else logger.warning("No playSoundEffect() calls found. No changes applied.")
    }
}
