package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceBatteryPresentPatch = bytecodePatch(
    name = "Force Battery Present",
    description = "Makes BatteryManager.isBatteryPresent() report true so apps that refuse to run without a battery work on batteryless devices.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/BatteryManager;" to mapOf("isBatteryPresent" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched battery-present check(s) to true")
        else logger.warning("No battery-present checks found. No changes applied.")
    }
}
