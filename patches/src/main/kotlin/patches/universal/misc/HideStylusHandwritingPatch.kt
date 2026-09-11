package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideStylusHandwritingPatch = bytecodePatch(
    name = "Hide Stylus Handwriting",
    description = "Makes stylus handwriting appear unavailable so apps hide handwriting UI.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
    val available by booleanOption(
        title = "Available",
        default = false,
        key = "stylusHandwriting",
        description = "Report stylus handwriting as available or not.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/view/inputmethod/InputMethodManager;",
            setOf("isStylusHandwritingAvailable"),
            available == true,
        )
        if (patched > 0) logger.info("Hid stylus handwriting at $patched call site(s)")
        else logger.warning("No stylus handwriting checks found. No changes applied.")
    }
}
