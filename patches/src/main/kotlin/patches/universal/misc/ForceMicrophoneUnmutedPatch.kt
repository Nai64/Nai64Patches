package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceMicrophoneUnmutedPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes AudioManager.isMicrophoneMute() report false so apps that pause or alter behavior when the mic is muted keep running normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isMicrophoneMute" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched microphone-mute check(s) to false")
        else logger.warning("No microphone-mute checks found. No changes applied.")
    }
}
