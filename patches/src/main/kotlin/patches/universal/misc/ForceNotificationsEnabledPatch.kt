package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofNotificationsEnabledPatch = bytecodePatch(
    name = "Spoof Notifications as Enabled",
    description = "Makes the app think notifications are enabled so it keeps working even if blocked.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/NotificationManager;" to mapOf("areNotificationsEnabled" to "0x1"),
            ),
        )

        if (patched > 0) logger.info("Forced $patched notification check(s) to enabled")
        else logger.warning("No notification-state checks found. No changes applied.")
    }
}
