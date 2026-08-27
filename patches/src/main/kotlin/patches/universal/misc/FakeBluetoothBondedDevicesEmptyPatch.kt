package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeBluetoothBondedDevicesEmptyPatch = bytecodePatch(
    name = "Fake Bluetooth Bonded Devices Empty",
    description = "Makes BluetoothAdapter.getBondedDevices() return an empty set so apps cannot fingerprint paired Bluetooth devices.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/bluetooth/BluetoothAdapter;",
            setOf("getBondedDevices"),
            "Ljava/util/Set;",
            "invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;",
        )
        if (patched > 0) {
            logger.info("Faked Bluetooth bonded devices at $patched call site(s)")
        } else {
            logger.warning("No getBondedDevices call sites found. No changes applied.")
        }
    }
}
