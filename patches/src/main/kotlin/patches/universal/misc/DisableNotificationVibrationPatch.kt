package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationVibrationPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces Notification(.Builder/Channel).setVibrate(...) to receive a null pattern so posted notifications do not vibrate.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setVibrate")) +
            forceNullParam("Landroid/app/NotificationChannel;", setOf("setVibrate")) +
            forceNullParam("Landroid/app/Notification;", setOf("setVibrate"))
        if (patched > 0) logger.info("Nulled $patched notification setVibrate() argument(s)")
        else logger.warning("No notification setVibrate() calls found. No changes applied.")
    }
}
