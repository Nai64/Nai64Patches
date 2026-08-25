package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSoundEffectsPatch = bytecodePatch(
    name = "Disable Sound Effects",
    description = "Forces AudioManager.setSoundEffectsEnabled(false) so system UI sound effects (locks, clicks, navigation) are silenced within the app.",
    default = false,
) {
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
