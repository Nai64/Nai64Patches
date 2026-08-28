package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideInputMethodsPatch = bytecodePatch(
    name = "Hide Input Methods",
    description = "Makes input methods appear empty so apps cannot fingerprint keyboards.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = replaceGetterWithStaticCall(
            "Landroid/view/inputmethod/InputMethodManager;",
            setOf("getEnabledInputMethodList", "getInputMethodList"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid input methods at $patched call site(s)")
        else logger.warning("No input method call sites found. No changes applied.")
    }
}
