package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeBluetoothEnabledPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes BluetoothAdapter.isEnabled always return true so apps that require Bluetooth " +
            "stop nagging about it being turned off",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/bluetooth/BluetoothAdapter;" to mapOf("isEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched bluetooth state check(s) to enabled")
        else logger.warning("No bluetooth checks found. No changes applied.")
    }
}
