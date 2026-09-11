package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableClipboardWritePatch = bytecodePatch(
    name = "Disable Clipboard Write",
    description = "No-ops ClipboardManager.setPrimaryClip(...) so the app cannot silently overwrite the system clipboard.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/content/ClipboardManager;", setOf("setPrimaryClip"))
        if (patched > 0) logger.info("Neutralized $patched setPrimaryClip() call(s)")
        else logger.warning("No setPrimaryClip() calls found. No changes applied.")
    }
}
