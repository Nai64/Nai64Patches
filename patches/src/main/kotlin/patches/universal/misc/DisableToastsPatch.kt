package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableToastsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops Toast.show() so the app cannot display toast pop-ups.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/widget/Toast;", setOf("show"))
        if (patched > 0) logger.info("Neutralized $patched Toast.show() call(s)")
        else logger.warning("No Toast.show() calls found. No changes applied.")
    }
}
