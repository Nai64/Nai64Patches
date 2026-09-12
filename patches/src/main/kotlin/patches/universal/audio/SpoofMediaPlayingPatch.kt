package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val spoofMediaPlayingPatch = bytecodePatch(
    name = "Spoof Media Playing",
    description = "Makes media-player checks report playing so playback-gated features stay active.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/MediaPlayer;" to mapOf("isPlaying" to "0x1")),
        )
        if (patched > 0) logger.info("Spoofed $patched media-playing check(s) to true")
        else logger.warning("No media-playing checks found. No changes applied.")
    }
}
