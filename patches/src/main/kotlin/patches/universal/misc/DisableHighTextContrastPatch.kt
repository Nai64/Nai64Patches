package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHighTextContrastPatch = bytecodePatch(
    name = "Disable High Text Contrast",
    description = "Makes AccessibilityManager.isHighTextContrastEnabled() report false so apps do not alter layouts for high-contrast text.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/view/accessibility/AccessibilityManager;" to mapOf("isHighTextContrastEnabled" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched high-text-contrast check(s) to false")
        else logger.warning("No high-text-contrast checks found. No changes applied.")
    }
}
