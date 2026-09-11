package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableScrollbarsPatch = bytecodePatch(
    name = "Disable Scrollbars",
    description = "Forces View.setVerticalScrollBarEnabled/setHorizontalScrollBarEnabled(false) so scrollbars are never drawn.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceBooleanValue(
            "Landroid/view/View;",
            setOf("setVerticalScrollBarEnabled", "setHorizontalScrollBarEnabled"),
            false,
        )
        if (patched > 0) logger.info("Forced $patched scrollbar-enable call(s) to false")
        else logger.warning("No scrollbar-enable calls found. No changes applied.")
    }
}
