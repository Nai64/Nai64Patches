package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import patches.universal.misc.forceBooleanValue
import java.util.logging.Logger

@Suppress("unused")
val forceSpeakerphoneOnPatch = bytecodePatch(
    name = "Force Speakerphone On",
    description = "Forces the speakerphone on and makes speakerphone checks report true.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceBooleanValue(
            "Landroid/media/AudioManager;",
            setOf("setSpeakerphoneOn"),
            true,
        ) + foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isSpeakerphoneOn" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched speakerphone call(s) on")
        else logger.warning("No speakerphone calls found. No changes applied.")
    }
}
