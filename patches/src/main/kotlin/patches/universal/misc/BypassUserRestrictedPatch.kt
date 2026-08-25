package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassUserRestrictedPatch = bytecodePatch(
    name = "Bypass User Restricted",
    description = "Makes UserManager.isUserRestricted() report false so apps that limit restricted accounts run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isUserRestricted" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched user-restricted check(s) to false")
        else logger.warning("No user-restricted checks found. No changes applied.")
    }
}
