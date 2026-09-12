package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.noOpVoidInvoke
import java.util.logging.Logger

@Suppress("unused")
val disableRingtonePlaybackPatch = bytecodePatch(
    name = "Disable Ringtone Playback",
    description = "Drops ringtone playback calls so ringtones stay silent.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Landroid/media/Ringtone;",
            setOf("play"),
        )
        if (patched > 0) logger.info("Disabled $patched ringtone call(s)")
        else logger.warning("No ringtone calls found. No changes applied.")
    }
}
