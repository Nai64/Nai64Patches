package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreGpsDisabledPatch = bytecodePatch(
    name = "Ignore GPS Disabled",
    description =
        "Makes LocationManager.isProviderEnabled always return true so apps that refuse to " +
            "run without location services keep working",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Ignore") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/location/LocationManager;" to mapOf("isProviderEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched provider check(s) to enabled")
        else logger.warning("No provider checks found. No changes applied.")
    }
}
