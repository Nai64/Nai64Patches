package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableKeyboardSoundPatch = bytecodePatch(
    name = "Disable Keyboard Sound",
    description = "Silences keyboard and system sounds.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/media/AudioManager;", setOf("playSoundEffect"))
        if (patched > 0) logger.info("Silenced $patched playSoundEffect() call(s)")
        else logger.warning("No playSoundEffect() calls found. No changes applied.")
    }
}
