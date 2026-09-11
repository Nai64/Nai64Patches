package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceVoiceCapablePatch = bytecodePatch(
    name = "Force Voice Capable",
    description = "Makes TelephonyManager.isVoiceCapable() report true so apps that require voice-call support run on data-only devices.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("isVoiceCapable" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched voice-capable check(s) to true")
        else logger.warning("No voice-capable checks found. No changes applied.")
    }
}
