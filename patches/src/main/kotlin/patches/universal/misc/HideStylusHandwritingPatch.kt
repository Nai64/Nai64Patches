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
