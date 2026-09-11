package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSoundEffectsPatch = bytecodePatch(
    name = "Disable Sound Effects",
    description = "Forces AudioManager.setSoundEffectsEnabled(false) so system UI sound effects (locks, clicks, navigation) are silenced within the app.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceBooleanValue(
            "Landroid/media/AudioManager;",
            setOf("setSoundEffectsEnabled"),
            false,
        )
        if (patched > 0) logger.info("Forced $patched setSoundEffectsEnabled() call(s) to false")
        else logger.warning("No setSoundEffectsEnabled() calls found. No changes applied.")
    }
}
