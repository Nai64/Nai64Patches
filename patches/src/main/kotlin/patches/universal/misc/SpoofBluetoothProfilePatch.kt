package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothProfilePatch = bytecodePatch(
    name = "Spoof Bluetooth Profile State",
    description = "Forces BluetoothAdapter.getProfileConnectionState to report connected so headset and A2DP gates proceed",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/bluetooth/BluetoothAdapter;" to mapOf("getProfileConnectionState" to "0x2")),
            returnType = "I",
        )
        if (patched > 0) logger.info("Spoofed Bluetooth profile state at $patched call site(s)")
        else logger.warning("No getProfileConnectionState calls found. No changes applied.")
    }
}
