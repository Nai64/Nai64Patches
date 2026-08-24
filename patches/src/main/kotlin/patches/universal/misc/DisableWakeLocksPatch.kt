package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableWakeLocksPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Prevents the app from holding PowerManager.WakeLock.acquire(...) so it can no longer keep the CPU/screen awake in the background, reducing battery drain.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/os/PowerManager\$WakeLock;", setOf("acquire"))
        if (patched > 0) logger.info("Neutralized $patched WakeLock.acquire() call(s)")
        else logger.warning("No WakeLock.acquire() calls found. No changes applied.")
    }
}
