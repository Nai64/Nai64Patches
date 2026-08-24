package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceVoiceCapablePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes TelephonyManager.isVoiceCapable() report true so apps that require voice-call support run on data-only devices.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isVoiceCapable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched voice-capable check(s) to true")
        else logger.warning("No voice-capable checks found. No changes applied.")
    }
}
