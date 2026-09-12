package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothAudioOnPatch = bytecodePatch(
    name = "Spoof Bluetooth Audio On",
    description = "Makes Bluetooth audio checks report connected so headset-gated features stay active.",
    default = false,
) {
    category("Spoof")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isBluetoothA2dpOn" to "0x1")),
        )
        if (patched > 0) logger.info("Spoofed $patched Bluetooth-audio check(s) to connected")
        else logger.warning("No Bluetooth-audio checks found. No changes applied.")
    }
}
