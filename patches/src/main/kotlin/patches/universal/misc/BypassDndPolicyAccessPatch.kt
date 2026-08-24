package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Forces android.app.NotificationManager.isNotificationPolicyAccessGranted() to
 * return true so automation and ringer-control apps that require Do Not Disturb
 * access work without the user granting it.
 */
@Suppress("unused")
val bypassDndPolicyAccessPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces NotificationManager.isNotificationPolicyAccessGranted() to return true so automation and ringer-control apps that require Do Not Disturb access work without granting it.",
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
            logger.info("Bypassed DND policy access at $patched call site(s)")
        } else {
            logger.warning("No NotificationManager.isNotificationPolicyAccessGranted call sites found. No changes applied.")
        }
    }
}
