package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideAccessibilityUsagePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes AccessibilityManager.isEnabled() report false so apps cannot tell an accessibility service is running.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/view/accessibility/AccessibilityManager;" to mapOf("isEnabled" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched accessibility check(s) to false")
        else logger.warning("No accessibility checks found. No changes applied.")
    }
}
