package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofDataStatePatch = bytecodePatch(
    name = "Spoof Data State",
    description = "Makes the app think mobile data is connected.",
    default = false,
) {
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
