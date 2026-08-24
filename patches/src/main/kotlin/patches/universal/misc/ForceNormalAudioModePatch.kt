package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceNormalAudioModePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes AudioManager.getMode() report MODE_NORMAL (0) so apps that change behavior in call/ringtone modes run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/media/AudioManager;",
            setOf("getMode"),
            0,
        )
        if (patched > 0) logger.info("Forced $patched audio-mode read(s) to NORMAL")
        else logger.warning("No getMode call sites found. No changes applied.")
    }
}
