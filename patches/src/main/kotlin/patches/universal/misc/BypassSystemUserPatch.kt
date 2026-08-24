package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassSystemUserPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes UserManager.isSystemUser() report false so apps gating behavior to the system user treat the current user as normal.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isSystemUser" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched system-user check(s) to false")
        else logger.warning("No system-user checks found. No changes applied.")
    }
}
