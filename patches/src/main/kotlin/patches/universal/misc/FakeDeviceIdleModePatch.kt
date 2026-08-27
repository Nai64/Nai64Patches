package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeDeviceIdleModePatch = bytecodePatch(
    name = "Fake Device Idle Mode",
    description = "Makes PowerManager.isDeviceIdleMode() report a chosen state so apps that gate features behind device idle checks stop doing so.",
    default = false,
) {
    val idle by booleanOption(
        title = "Report idle",
        default = false,
        key = "deviceIdle",
        description = "Report device as idle (true) or not idle (false). Recommended: false.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/os/PowerManager;",
            setOf("isDeviceIdleMode"),
            idle == true,
        )
        if (patched > 0) {
            logger.info("Faked device idle mode at $patched call site(s)")
        } else {
            logger.warning("No isDeviceIdleMode call sites found. No changes applied.")
        }
    }
}
