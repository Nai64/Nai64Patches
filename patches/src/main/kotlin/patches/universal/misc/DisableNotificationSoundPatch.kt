package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationSoundPatch = bytecodePatch(
    name = "Disable Notification Sound",
    description = "Mutes notification sounds.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setSound")) +
            forceNullParam("Landroid/app/NotificationChannel;", setOf("setSound")) +
            forceNullParam("Landroid/app/Notification;", setOf("setSound"))
        if (patched > 0) logger.info("Nulled $patched notification setSound() Uri argument(s)")
        else logger.warning("No notification setSound() calls found. No changes applied.")
    }
}
