package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableActivityTransitionsPatch = bytecodePatch(
    name = "Disable Activity Transitions",
    description = "No-ops Activity.overridePendingTransition(...) so the app's custom activity enter/exit animations are skipped.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/Activity;", setOf("overridePendingTransition"))
        if (patched > 0) logger.info("Neutralized $patched overridePendingTransition() call(s)")
        else logger.warning("No overridePendingTransition() calls found. No changes applied.")
    }
}
