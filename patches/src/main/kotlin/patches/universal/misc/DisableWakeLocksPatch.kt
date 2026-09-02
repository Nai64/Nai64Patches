package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableWakeLocksPatch = bytecodePatch(
    name = "Disable Wake Locks",
    description = "Stops the app from keeping the screen awake.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/os/PowerManager\$WakeLock;", setOf("acquire"))
        if (patched > 0) logger.info("Neutralized $patched WakeLock.acquire() call(s)")
        else logger.warning("No WakeLock.acquire() calls found. No changes applied.")
    }
}
