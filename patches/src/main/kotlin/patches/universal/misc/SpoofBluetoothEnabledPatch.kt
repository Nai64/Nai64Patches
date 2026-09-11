package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBluetoothEnabledPatch = bytecodePatch(
    name = "Spoof Bluetooth Enabled",
    description =
        "Forces Settings.Global BLUETOOTH_ON reads to 0 (off) so apps that behave " +
            "differently when Bluetooth is enabled cannot tell it is on.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
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
