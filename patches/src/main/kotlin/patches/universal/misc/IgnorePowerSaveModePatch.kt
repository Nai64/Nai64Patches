package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignorePowerSaveModePatch = bytecodePatch(
    name = "Ignore Power Save Mode",
    description =
        "Makes PowerManager.isPowerSaveMode and isDeviceIdleMode always return false so apps " +
            "do not throttle features or downloads while battery saver or Doze is active",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Ignore") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/os/PowerManager;" to mapOf(
                    "isPowerSaveMode" to "0x0",
                    "isDeviceIdleMode" to "0x0",
                ),
            ),
        )

        if (patched > 0) logger.info("Forced $patched power-save check(s) to off")
        else logger.warning("No power-save checks found. No changes applied.")
    }
}
