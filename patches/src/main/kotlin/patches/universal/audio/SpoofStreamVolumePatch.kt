package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import patches.universal.misc.foldIntGetterConst
import java.util.logging.Logger

@Suppress("unused")
val spoofStreamVolumePatch = bytecodePatch(
    name = "Spoof Stream Volume",
    description = "Reports a chosen level for stream-volume reads so apps behave as if the volume is set there.",
    default = false,
) {
    category("Audio")
    val volumeLevel by intOption(
        title = "Volume level",
        default = 15,
        key = "spoofStreamVolume",
        description = "Reported AudioManager.getStreamVolume value.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val level = volumeLevel ?: 15
        val patched = foldIntGetterConst(
            "Landroid/media/AudioManager;",
            setOf("getStreamVolume"),
            level,
        )
        if (patched > 0) logger.info("Spoofed $patched stream-volume check(s) to $level")
        else logger.warning("No stream-volume checks found. No changes applied.")
    }
}
