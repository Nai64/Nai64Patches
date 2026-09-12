package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.noOpVoidInvoke
import java.util.logging.Logger

@Suppress("unused")
val disableWindowBlurPatch = bytecodePatch(
    name = "Disable Window Blur",
    description = "Drops window and view blur calls for sharper UI and better performance.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke(
            targetClass = "Landroid/view/Window;",
            methods = setOf("setBackgroundBlurRadius"),
        )
        patched += noOpVoidInvoke(
            targetClass = "Landroid/view/View;",
            methods = setOf("setRenderEffect"),
        )
        if (patched > 0) logger.info("Disabled $patched blur call(s)")
        else logger.warning("No blur calls found. No changes applied.")
    }
}
