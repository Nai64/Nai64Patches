package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceStorageNonRemovablePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes Environment.isExternalStorageRemovable() report false so apps that refuse to run from removable storage treat it as internal.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/Environment;" to mapOf("isExternalStorageRemovable" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched removable-storage check(s) to false")
        else logger.warning("No removable-storage checks found. No changes applied.")
    }
}
