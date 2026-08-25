package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableBluetoothDiscoveringPatch = bytecodePatch(
    name = "Disable Bluetooth Discovering",
    description = "Makes BluetoothAdapter.isDiscovering() report false so apps cannot tell the adapter is in discovery mode.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/bluetooth/BluetoothAdapter;" to mapOf("isDiscovering" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched bluetooth-discovering check(s) to false")
        else logger.warning("No bluetooth-discovering checks found. No changes applied.")
    }
}
