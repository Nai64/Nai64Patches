package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

/**
 * Forces android.provider.Settings.canDrawOverlays() to return true at every
 * call site, so apps that refuse to run while an overlay (screen recorder,
 * accessibility service, chat heads, etc.) is present stop disabling
 * themselves.
 */
@Suppress("unused")
val bypassOverlayDetectionPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces android.provider.Settings.canDrawOverlays() to return true so apps that block screen recorders, accessibility overlays or chat heads stop disabling themselves.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/provider/Settings;",
            setOf("canDrawOverlays"),
            true,
        )
        if (patched > 0) {
            logger.info("Bypassed overlay detection at $patched call site(s)")
        } else {
            logger.warning("No Settings.canDrawOverlays call sites found. No changes applied.")
        }
    }
}
