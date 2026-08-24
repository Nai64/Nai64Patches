package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassPackageSuspendedPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes PackageManager.isPackageSuspended() report false so apps that refuse to run while suspended keep working.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("isPackageSuspended" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched package-suspended check(s) to false")
        else logger.warning("No package-suspended checks found. No changes applied.")
    }
}
