package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableWakeLocksPatch = bytecodePatch(
    name = "Disable Wake Locks",
    description = "Stops the app from keeping the screen awake.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/os/PowerManager\$WakeLock;", setOf("acquire"))
        if (patched > 0) logger.info("Neutralized $patched WakeLock.acquire() call(s)")
        else logger.warning("No WakeLock.acquire() calls found. No changes applied.")
    }
}
