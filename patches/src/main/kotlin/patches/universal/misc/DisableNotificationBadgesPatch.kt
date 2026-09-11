package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableNotificationBadgesPatch = bytecodePatch(
    name = "Disable Notification Badges",
    description = "Remove launcher badge dots from notification channels.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    val disabled by booleanOption(
        title = "Disable badges",
        default = true,
        key = "notificationBadgesDisabled",
        description = "Force channels to not show badges (true) or leave them alone (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = if (disabled == true) {
            forceBooleanValue(
                "Landroid/app/NotificationChannel;",
                setOf("setShowBadge"),
                false,
            )
        } else 0
        if (patched > 0) {
            logger.info("Disabled notification badges at $patched call site(s)")
        } else {
            logger.warning("No NotificationChannel.setShowBadge call sites found. No changes applied.")
        }
    }
}
