package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothEnabledPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Forces Settings.Global BLUETOOTH_ON reads to 0 (off) so apps that behave " +
            "differently when Bluetooth is enabled cannot tell it is on.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldSettingsGetterConst(
            classes = setOf("Landroid/provider/Settings\$Global;"),
            keys = setOf("bluetooth_on"),
        )

        if (patched > 0) {
            logger.info("Spoofed $patched Bluetooth-enabled read(s)")
        } else {
            logger.warning("No Bluetooth-enabled reads found. No changes applied.")
        }
    }
}
