package patches.universal.ui

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.forceBooleanValue
import java.util.logging.Logger

@Suppress("unused")
val alwaysShowScrollbarsPatch = bytecodePatch(
    name = "Always Show Scrollbars",
    description = "Forces scrollbars to stay enabled so scroll position is always visible",
    default = false,
) {
    category("Interface")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceBooleanValue(
            "Landroid/view/View;",
            setOf("setVerticalScrollBarEnabled", "setHorizontalScrollBarEnabled"),
            true,
        )
        if (patched > 0) logger.info("Forced $patched scrollbar-enable call(s) to true")
        else logger.warning("No scrollbar-enable calls found. No changes applied.")
    }
}
