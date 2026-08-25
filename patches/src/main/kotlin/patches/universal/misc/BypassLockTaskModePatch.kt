package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassLockTaskModePatch = bytecodePatch(
    name = "Bypass Lock Task Mode",
    description = "Makes ActivityManager.isInLockTaskMode() report false so apps that change behavior under screen-pinning or kiosk mode run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/ActivityManager;" to mapOf("isInLockTaskMode" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched lock-task-mode check(s) to false")
        else logger.warning("No lock-task-mode checks found. No changes applied.")
    }
}
