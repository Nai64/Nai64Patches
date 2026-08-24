package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableOverscrollEffectPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces View.setOverScrollMode(OVER_SCROLL_NEVER) so the edge glow/bounce at list boundaries is disabled.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceIntValue("Landroid/view/View;", setOf("setOverScrollMode"), 2)
        if (patched > 0) logger.info("Forced $patched setOverScrollMode() call(s) to OVER_SCROLL_NEVER")
        else logger.warning("No setOverScrollMode() calls found. No changes applied.")
    }
}
