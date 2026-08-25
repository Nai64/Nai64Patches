package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationsPatch = bytecodePatch(
    name = "Disable Notifications",
    description = "No-ops NotificationManager.notify(...) so the app cannot post any notifications at all.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/NotificationManager;", setOf("notify"))
        if (patched > 0) logger.info("Neutralized $patched notify() call(s)")
        else logger.warning("No notify() calls found. No changes applied.")
    }
}
