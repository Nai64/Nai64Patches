package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceNotificationsEnabledPatch = bytecodePatch(
    name = "Force Notifications Enabled",
    description =
        "Makes NotificationManager.areNotificationsEnabled always return true so apps that " +
            "refuse to run when notifications are blocked keep working",
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
