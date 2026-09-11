package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceSpeakerphoneOffPatch = bytecodePatch(
    name = "Force Speakerphone Off",
    description = "Makes AudioManager.isSpeakerphoneOn() report false so apps that react to speakerphone state behave as if it is off.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isSpeakerphoneOn" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched speakerphone check(s) to false")
        else logger.warning("No speakerphone checks found. No changes applied.")
    }
}
