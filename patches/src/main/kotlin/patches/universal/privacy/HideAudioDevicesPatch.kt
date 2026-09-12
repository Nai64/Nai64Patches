package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceArrayGetterWithEmpty

@Suppress("unused")
val hideAudioDevicesPatch = bytecodePatch(
    name = "Hide Audio Devices",
    description = "Reports no audio devices so apps cannot enumerate microphones, speakers or Bluetooth audio routes",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceArrayGetterWithEmpty(
            "Landroid/media/AudioManager;",
            setOf("getDevices"),
            "Landroid/media/AudioDeviceInfo;",
        )
        if (patched > 0) logger.info("Hid audio devices at $patched call site(s)")
        else logger.warning("No audio device calls found. No changes applied.")
    }
}
