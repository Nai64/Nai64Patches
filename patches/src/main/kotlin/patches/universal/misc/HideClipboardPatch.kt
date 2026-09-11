package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideClipboardPatch = bytecodePatch(
    name = "Hide Clipboard",
    description = "Forces ClipboardManager.getPrimaryClip() to return null so the app cannot read the system clipboard.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
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
