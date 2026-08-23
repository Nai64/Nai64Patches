package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothNamePatch = bytecodePatch(
    name = "Spoof Bluetooth Name",
    description = "Reports a constant name from BluetoothAdapter.getName() so apps cannot read the real local Bluetooth adapter name.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/bluetooth/BluetoothAdapter;",
            setOf("getName"),
            "Unknown",
        )
        if (patched > 0) logger.info("Spoofed Bluetooth name at $patched call site(s)")
        else logger.warning("No BluetoothAdapter.getName call sites found. No changes applied.")
    }
}
