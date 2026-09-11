package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHeadsUpNotificationsPatch = bytecodePatch(
    name = "Disable Heads-up Notifications",
    description = "Blocks pop-up notifications.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceNullParam("Landroid/app/Notification\$Builder;", setOf("setFullScreenIntent")) +
            forceNullParam("Landroid/app/Notification;", setOf("setFullScreenIntent"))
        if (patched > 0) logger.info("Nulled $patched setFullScreenIntent() argument(s)")
        else logger.warning("No setFullScreenIntent() calls found. No changes applied.")
    }
}
