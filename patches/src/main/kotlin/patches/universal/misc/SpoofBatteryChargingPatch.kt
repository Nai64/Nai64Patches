package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Forces android.os.BatteryManager.isCharging() to return true so apps that
 * restrict features unless the device is charging stop doing so.
 */
@Suppress("unused")
val spoofBatteryChargingPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces BatteryManager.isCharging() to return true so apps that restrict features unless the device is charging stop doing so.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/os/BatteryManager;",
            setOf("isCharging"),
            true,
        )
        if (patched > 0) {
            logger.info("Spoofed charging state at $patched call site(s)")
        } else {
            logger.warning("No BatteryManager.isCharging call sites found. No changes applied.")
        }
    }
}
