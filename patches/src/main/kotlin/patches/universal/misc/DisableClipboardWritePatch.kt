package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableClipboardWritePatch = bytecodePatch(
    name = "Disable Clipboard Write",
    description = "No-ops ClipboardManager.setPrimaryClip(...) so the app cannot silently overwrite the system clipboard.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/content/ClipboardManager;", setOf("setPrimaryClip"))
        if (patched > 0) logger.info("Neutralized $patched setPrimaryClip() call(s)")
        else logger.warning("No setPrimaryClip() calls found. No changes applied.")
    }
}
