package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideClipboardPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces ClipboardManager.getPrimaryClip() to return null so the app cannot read the system clipboard.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Landroid/content/ClipboardManager;",
            setOf("getPrimaryClip"),
            "Landroid/content/ClipData;",
        )
        if (patched > 0) logger.info("Nulled $patched clipboard read(s)")
        else logger.warning("No clipboard reads found. No changes applied.")
    }
}
