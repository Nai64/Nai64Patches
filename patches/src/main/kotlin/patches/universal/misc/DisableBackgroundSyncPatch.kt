package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableBackgroundSyncPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops ContentResolver.setSyncAutomatically/requestSync(...) so the app stops auto-uploading data (contacts, photos, files) to cloud accounts in the background.",
    default = false,
) {
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
