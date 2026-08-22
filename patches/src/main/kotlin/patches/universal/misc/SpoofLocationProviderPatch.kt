package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofLocationProviderPatch = bytecodePatch(
    name = "Spoof Location Provider",
    description =
        "Forces LocationManager.getBestProvider() to report \"gps\" and " +
            "isProviderEnabled() to report true, so location-dependent apps believe a GPS " +
            "provider is always available and enabled.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val best = foldStringGetterConst(
            definingClass = "Landroid/location/LocationManager;",
            methodNames = setOf("getBestProvider"),
            value = "gps",
        )
        val enabled = foldBooleanGetterConst(
            definingClass = "Landroid/location/LocationManager;",
            methodNames = setOf("isProviderEnabled"),
            value = true,
        )

        val total = best + enabled
        if (total > 0) {
            logger.info("Spoofed $total location-provider read(s)")
        } else {
            logger.warning("No location-provider reads found. No changes applied.")
        }
    }
}
