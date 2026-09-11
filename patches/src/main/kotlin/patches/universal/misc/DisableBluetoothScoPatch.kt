package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableBluetoothScoPatch = bytecodePatch(
    name = "Disable Bluetooth SCO",
    description = "Makes AudioManager.isBluetoothScoOn() report false so apps that change behavior when a Bluetooth SCO link is active run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isBluetoothScoOn" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched bluetooth-sco check(s) to false")
        else logger.warning("No bluetooth-sco checks found. No changes applied.")
    }
}
