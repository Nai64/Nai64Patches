package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofNotificationsEnabledPatch = bytecodePatch(
    name = "Spoof Notifications as Enabled",
    description = "Makes the app think notifications are enabled so it keeps working even if blocked.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/NotificationManager;" to mapOf("areNotificationsEnabled" to "0x1"),
            ),
        )

        if (patched > 0) logger.info("Forced $patched notification check(s) to enabled")
        else logger.warning("No notification-state checks found. No changes applied.")
    }
}
