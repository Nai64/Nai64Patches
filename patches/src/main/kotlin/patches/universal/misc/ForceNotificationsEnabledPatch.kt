package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofNotificationsEnabledPatch = bytecodePatch(
    name = "Spoof Notifications as Enabled",
    description =
        "Makes NotificationManager.areNotificationsEnabled always return true so apps that " +
            "refuse to run when notifications are blocked keep working. This only changes the " +
            "app's internal check; it does not grant POST_NOTIFICATIONS or enable notifications " +
            "in Android system settings. Use Ensure Notification Permission for the manifest entry.",
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
