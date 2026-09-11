package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeNotificationListenerPatch = bytecodePatch(
    name = "Fake Notification Listener",
    description = "Report notification-listener access as granted.",
    default = false,
) {
    category("Fake")
    val granted by booleanOption(
        title = "Grant access",
        default = true,
        key = "notificationListenerAccess",
        description = "Report notification-listener access as granted (true) or denied (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = foldBooleanGetterConst(
            "Landroid/app/NotificationManager;",
            setOf("isNotificationListenerAccessGranted"),
            granted == true,
        )
        patched += foldBooleanGetterConst(
            "Landroidx/core/app/NotificationManagerCompat;",
            setOf("isNotificationListenerAccessGranted"),
            granted == true,
        )
        if (patched > 0) {
            logger.info("Faked notification-listener access at $patched call site(s)")
        } else {
            logger.warning("No notification-listener grant call sites found. No changes applied.")
        }
    }
}
