package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassKeyguardSecurePatch = bytecodePatch(
    name = "Bypass Keyguard Secure",
    description = "Makes KeyguardManager.isKeyguardSecure() report false so apps that require a secure lock screen run without one.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bypass") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/KeyguardManager;" to mapOf("isKeyguardSecure" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched keyguard-secure check(s) to false")
        else logger.warning("No keyguard-secure checks found. No changes applied.")
    }
}
