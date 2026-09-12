package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val disableGameSoundsPatch = bytecodePatch(
    name = "Disable Game Sounds",
    description = "Makes SoundPool playback return an invalid stream so short game sounds stay silent.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/SoundPool;" to mapOf("play" to "0x0")),
            returnType = "I",
        )
        if (patched > 0) logger.info("Disabled $patched game-sound call(s)")
        else logger.warning("No game-sound calls found. No changes applied.")
    }
}
