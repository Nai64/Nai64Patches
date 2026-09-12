package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disablePopupKeyboardPatch = bytecodePatch(
    name = "Disable Popup Keyboard",
    description = "Makes soft-keyboard requests report failure so the keyboard stops popping up.",
    default = false,
) {
    category("Disable")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/view/inputmethod/InputMethodManager;" to mapOf("showSoftInput" to "0x0")),
        )
        if (patched > 0) logger.info("Blocked $patched keyboard request(s)")
        else logger.warning("No keyboard requests found. No changes applied.")
    }
}
