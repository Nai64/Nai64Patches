package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassGuestUserPatch = bytecodePatch(
    name = "Bypass Guest User",
    description = "Makes UserManager.isGuestUser() report false so apps that limit guest accounts behave as on a primary account.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isGuestUser" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched guest-user check(s) to false")
        else logger.warning("No guest-user checks found. No changes applied.")
    }
}
