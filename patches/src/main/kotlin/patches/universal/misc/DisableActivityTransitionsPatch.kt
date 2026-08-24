package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableActivityTransitionsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops Activity.overridePendingTransition(...) so the app's custom activity enter/exit animations are skipped.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/Activity;", setOf("overridePendingTransition"))
        if (patched > 0) logger.info("Neutralized $patched overridePendingTransition() call(s)")
        else logger.warning("No overridePendingTransition() calls found. No changes applied.")
    }
}
