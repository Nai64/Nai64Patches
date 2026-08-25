package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHeadsUpNotificationsPatch = bytecodePatch(
    name = "Disable Heads-up Notifications",
    description = "Forces Notification(.Builder).setFullScreenIntent(...) to receive a null PendingIntent so heads-up/peek pop-overs no longer appear.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setFullScreenIntent")) +
            forceNullParam("Landroid/app/Notification;", setOf("setFullScreenIntent"))
        if (patched > 0) logger.info("Nulled $patched setFullScreenIntent() argument(s)")
        else logger.warning("No setFullScreenIntent() calls found. No changes applied.")
    }
}
