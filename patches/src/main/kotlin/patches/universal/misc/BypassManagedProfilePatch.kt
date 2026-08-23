package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassManagedProfilePatch = bytecodePatch(
    name = "Bypass Managed Profile",
    description = "Makes UserManager.isManagedProfile() report false so apps that restrict features on work/managed profiles behave as on a normal profile.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isManagedProfile" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched managed-profile check(s) to false")
        else logger.warning("No managed-profile checks found. No changes applied.")
    }
}
