package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.noOpVoidInvoke
import java.util.logging.Logger

@Suppress("unused")
val disableAudioRecordingPatch = bytecodePatch(
    name = "Disable Audio Recording",
    description = "Drops microphone recording calls so the app cannot capture audio.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Landroid/media/MediaRecorder;",
            setOf("setAudioSource", "start"),
        ) + noOpVoidInvoke(
            "Landroid/media/AudioRecord;",
            setOf("startRecording"),
        )
        if (patched > 0) logger.info("Disabled $patched audio-recording call(s)")
        else logger.warning("No audio-recording calls found. No changes applied.")
    }
}
