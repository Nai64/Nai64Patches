package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHapticOnScrollPatch = bytecodePatch(
    name = "Disable Haptic On Scroll",
    description = "Prevents haptic feedback triggered by scrolling.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = foldBooleanReturns(
            mapOf(
                "Landroid/view/View;" to mapOf("performHapticFeedback" to "0x0"),
            ),
            returnType = "Z",
        )
        // Also cover HapticFeedbackConstants direct checks if any
        patched += foldBooleanGetterConst(
            "Landroid/view/HapticFeedbackConstants;",
            setOf("isHapticFeedbackEnabled"),
            false,
        )
        if (patched > 0) logger.info("Disabled haptic on scroll at $patched call site(s)")
        else logger.warning("No haptic feedback call sites found. No changes applied.")
    }
}
