package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassSafeModePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes PackageManager.isSafeMode() report false so apps that disable functionality in safe mode behave normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("isSafeMode" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched safe-mode check(s) to false")
        else logger.warning("No safe-mode checks found. No changes applied.")
    }
}
