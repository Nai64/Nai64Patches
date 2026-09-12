package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val silenceNotificationChannelsPatch = bytecodePatch(
    name = "Silence Notification Channels",
    description = "Forces notification channel importance to none so channels stay silent.",
    default = false,
) {
    category("Disable")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceIntValue(
            "Landroid/app/NotificationChannel;",
            setOf("setImportance"),
            0,
        )
        if (patched > 0) logger.info("Silenced $patched notification channel(s)")
        else logger.warning("No notification channel importance calls found. No changes applied.")
    }
}
