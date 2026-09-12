package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull
import patches.universal.misc.noOpVoidInvoke

@Suppress("unused")
val blockBleScanPatch = bytecodePatch(
    name = "Block BLE Scan",
    description = "Blocks Bluetooth Low Energy scans so apps cannot track nearby beacons or accessories",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke(
            "Landroid/bluetooth/le/BluetoothLeScanner;",
            setOf("startScan", "stopScan"),
        )
        patched += foldObjectGetterToNull(
            "Landroid/bluetooth/BluetoothAdapter;",
            setOf("getBluetoothLeScanner"),
            "Landroid/bluetooth/le/BluetoothLeScanner;",
        )
        if (patched > 0) logger.info("Blocked BLE scan at $patched call site(s)")
        else logger.warning("No BLE scan calls found. No changes applied.")
    }
}
