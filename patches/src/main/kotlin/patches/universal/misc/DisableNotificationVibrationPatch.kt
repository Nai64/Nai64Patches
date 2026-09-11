package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationVibrationPatch = bytecodePatch(
    name = "Disable Notification Vibration",
    description = "Forces Notification(.Builder/Channel).setVibrate(...) to receive a null pattern so posted notifications do not vibrate.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setVibrate")) +
            forceNullParam("Landroid/app/NotificationChannel;", setOf("setVibrate")) +
            forceNullParam("Landroid/app/Notification;", setOf("setVibrate"))
        if (patched > 0) logger.info("Nulled $patched notification setVibrate() argument(s)")
        else logger.warning("No notification setVibrate() calls found. No changes applied.")
    }
}
