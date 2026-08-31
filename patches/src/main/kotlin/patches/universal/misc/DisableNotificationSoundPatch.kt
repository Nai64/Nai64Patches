package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationSoundPatch = bytecodePatch(
    name = "Disable Notification Sound",
    description = "Mutes notification sounds.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setSound")) +
            forceNullParam("Landroid/app/NotificationChannel;", setOf("setSound")) +
            forceNullParam("Landroid/app/Notification;", setOf("setSound"))
        if (patched > 0) logger.info("Nulled $patched notification setSound() Uri argument(s)")
        else logger.warning("No notification setSound() calls found. No changes applied.")
    }
}
