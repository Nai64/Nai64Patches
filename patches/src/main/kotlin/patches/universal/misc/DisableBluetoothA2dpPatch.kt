package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableBluetoothA2dpPatch = bytecodePatch(
    name = "Disable Bluetooth A2DP",
    description = "Makes AudioManager.isBluetoothA2dpOn() report false so apps that change behavior when Bluetooth audio is streaming run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isBluetoothA2dpOn" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched bluetooth-a2dp check(s) to false")
        else logger.warning("No bluetooth-a2dp checks found. No changes applied.")
    }
}
