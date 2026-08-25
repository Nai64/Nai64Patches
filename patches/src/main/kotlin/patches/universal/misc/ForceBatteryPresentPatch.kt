package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceBatteryPresentPatch = bytecodePatch(
    name = "Force Battery Present",
    description = "Makes BatteryManager.isBatteryPresent() report true so apps that refuse to run without a battery work on batteryless devices.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/BatteryManager;" to mapOf("isBatteryPresent" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched battery-present check(s) to true")
        else logger.warning("No battery-present checks found. No changes applied.")
    }
}
