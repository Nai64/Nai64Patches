package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothNamePatch = bytecodePatch(
    name = "Spoof Bluetooth Name",
    description = "Reports a constant name from BluetoothAdapter.getName() so apps cannot read the real local Bluetooth adapter name.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
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
