package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableToastsPatch = bytecodePatch(
    name = "Disable Toasts",
    description = "No-ops Toast.show() so the app cannot display toast pop-ups.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/widget/Toast;", setOf("show"))
        if (patched > 0) logger.info("Neutralized $patched Toast.show() call(s)")
        else logger.warning("No Toast.show() calls found. No changes applied.")
    }
}
