package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassLinkedUserPatch = bytecodePatch(
    name = "Bypass Linked User",
    description = "Makes UserManager.isLinkedUser() report false so apps that restrict linked/secondary profiles run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isLinkedUser" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched linked-user check(s) to false")
        else logger.warning("No linked-user checks found. No changes applied.")
    }
}
