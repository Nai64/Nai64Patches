package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableCameraShutterSoundPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Silences the camera shutter and video-record sounds by neutralizing MediaActionSound.play(...) and Camera.playSound(...).",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/media/MediaActionSound;", setOf("play")) +
            noOpVoidInvoke("Landroid/hardware/Camera;", setOf("playSound"))
        if (patched > 0) logger.info("Silenced $patched camera sound call(s)")
        else logger.warning("No camera sound calls found. No changes applied.")
    }
}
