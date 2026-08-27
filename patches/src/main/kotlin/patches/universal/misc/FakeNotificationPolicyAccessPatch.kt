package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Makes NotificationManager.isNotificationPolicyAccessGranted() return true so
 * apps that gate features behind DND policy access stop doing so.
 */
@Suppress("unused")
val fakeNotificationPolicyAccessPatch = bytecodePatch(
    name = "Fake Notification Policy Access",
    description = "Makes NotificationManager.isNotificationPolicyAccessGranted() return true so apps that gate features behind DND policy access stop doing so.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/app/NotificationManager;",
            setOf("isNotificationPolicyAccessGranted"),
            true,
        )
        if (patched > 0) {
            logger.info("Faked notification policy access at $patched call site(s)")
        } else {
            logger.warning("No isNotificationPolicyAccessGranted call sites found. No changes applied.")
        }
    }
}
