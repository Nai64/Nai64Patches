package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofDataStatePatch = bytecodePatch(
    name = "Spoof Data State",
    description = "Makes the app think mobile data is connected.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val state = foldNoArgIntGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getDataState"),
            value = 2, // TelephonyManager.DATA_CONNECTED
        )
        val activity = foldNoArgIntGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getDataActivity"),
            value = 3, // TelephonyManager.DATA_ACTIVITY_INOUT
        )

        val total = state + activity
        if (total > 0) {
            logger.info("Spoofed $total data-state read(s)")
        } else {
            logger.warning("No data-state reads found. No changes applied.")
        }
    }
}
