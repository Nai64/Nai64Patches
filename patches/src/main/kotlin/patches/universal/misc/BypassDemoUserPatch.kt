package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassDemoUserPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes UserManager.isDemoUser() report false so apps that gate functionality in retail/demo mode run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isDemoUser" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched demo-user check(s) to false")
        else logger.warning("No demo-user checks found. No changes applied.")
    }
}
