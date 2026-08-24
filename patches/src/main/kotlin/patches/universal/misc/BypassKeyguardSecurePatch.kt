package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassKeyguardSecurePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes KeyguardManager.isKeyguardSecure() report false so apps that require a secure lock screen run without one.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/KeyguardManager;" to mapOf("isKeyguardSecure" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched keyguard-secure check(s) to false")
        else logger.warning("No keyguard-secure checks found. No changes applied.")
    }
}
