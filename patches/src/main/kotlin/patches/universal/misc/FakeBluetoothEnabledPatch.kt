package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeBluetoothEnabledPatch = bytecodePatch(
    name = "Fake Bluetooth Enabled",
    description =
        "Makes BluetoothAdapter.isEnabled always return true so apps that require Bluetooth " +
            "stop nagging about it being turned off",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Fake") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/bluetooth/BluetoothAdapter;" to mapOf("isEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched bluetooth state check(s) to enabled")
        else logger.warning("No bluetooth checks found. No changes applied.")
    }
}
