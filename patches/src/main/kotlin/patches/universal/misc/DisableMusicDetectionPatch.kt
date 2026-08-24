package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableMusicDetectionPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes AudioManager.isMusicActive() report false so apps that react to playing music behave as if nothing is playing.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isMusicActive" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched music-active check(s) to false")
        else logger.warning("No music-active checks found. No changes applied.")
    }
}
