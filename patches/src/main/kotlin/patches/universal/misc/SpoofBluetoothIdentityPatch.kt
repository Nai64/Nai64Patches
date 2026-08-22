package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothIdentityPatch = bytecodePatch(
    name = "Spoof Bluetooth Identity",
    description =
        "Reports chosen values from BluetoothAdapter.getName() and getAddress() so apps " +
            "cannot read the real Bluetooth device name or MAC address.",
    default = false,
) {
    val name by stringOption(
        title = "Bluetooth name",
        default = "Unknown",
        key = "spoofBluetoothName",
        description = "Bluetooth device name to report.",
    )
    val address by stringOption(
        title = "Bluetooth address",
        default = "00:00:00:00:00:00",
        key = "spoofBluetoothAddress",
        description = "Bluetooth MAC address to report (XX:XX:XX:XX:XX).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val n = (name ?: "Unknown").trim()
        val a = (address ?: "00:00:00:00:00:00").trim()

        val patchedName = foldNoArgStringGetter(
            definingClass = "Landroid/bluetooth/BluetoothAdapter;",
            methodNames = setOf("getName"),
            value = n,
        )
        val patchedAddress = foldNoArgStringGetter(
            definingClass = "Landroid/bluetooth/BluetoothAdapter;",
            methodNames = setOf("getAddress"),
            value = a,
        )

        val total = patchedName + patchedAddress
        if (total > 0) {
            logger.info("Spoofed $total Bluetooth identity read(s)")
        } else {
            logger.warning("No Bluetooth identity reads found. No changes applied.")
        }
    }
}
