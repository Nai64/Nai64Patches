package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceDoNotDisturbOffPatch = bytecodePatch(
    name = "Force Do Not Disturb Off",
    description = "Makes interruption-filter checks report all so apps behave as if Do Not Disturb is off.",
    default = false,
) {
    category("Force")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/app/NotificationManager;",
            setOf("getCurrentInterruptionFilter"),
            1,
        )
        if (patched > 0) logger.info("Forced $patched interruption-filter check(s) to all")
        else logger.warning("No interruption-filter checks found. No changes applied.")
    }
}
