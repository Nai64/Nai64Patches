package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceUserUnlockedPatch = bytecodePatch(
    name = "Force User Unlocked",
    description = "Makes UserManager.isUserUnlocked() report true so direct-boot restricted features and storage become available immediately.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/UserManager;" to mapOf("isUserUnlocked" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched user-unlocked check(s) to true")
        else logger.warning("No user-unlocked checks found. No changes applied.")
    }
}
