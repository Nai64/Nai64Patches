package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableBackgroundSyncPatch = bytecodePatch(
    name = "Disable Background Sync",
    description = "Stops background uploads to cloud accounts.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Landroid/content/ContentResolver;",
            setOf("setSyncAutomatically", "requestSync"),
        )
        if (patched > 0) logger.info("Neutralized $patched background-sync call(s)")
        else logger.warning("No background-sync calls found. No changes applied.")
    }
}
