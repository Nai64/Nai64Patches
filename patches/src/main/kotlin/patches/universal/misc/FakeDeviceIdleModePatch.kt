package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Makes PowerManager.isDeviceIdleMode() return false so apps that gate features
 * behind device idle checks stop doing so.
 */
@Suppress("unused")
val fakeDeviceIdleModePatch = bytecodePatch(
    name = "Fake Device Idle Mode",
    description = "Makes PowerManager.isDeviceIdleMode() return false so apps that gate features behind device idle checks stop doing so.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/os/PowerManager;",
            setOf("isDeviceIdleMode"),
            false,
        )
        if (patched > 0) {
            logger.info("Faked device idle mode at $patched call site(s)")
        } else {
            logger.warning("No isDeviceIdleMode call sites found. No changes applied.")
        }
    }
}
