package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val disableTextToSpeechPatch = bytecodePatch(
    name = "Disable Text-to-Speech",
    description = "Reports text-to-speech success without playing anything so spoken prompts stay silent.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/speech/tts/TextToSpeech;" to mapOf(
                    "speak" to "0x0",
                    "playSilentUtterance" to "0x0",
                    "playSilence" to "0x0",
                ),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Silenced $patched text-to-speech call(s)")
        else logger.warning("No text-to-speech calls found. No changes applied.")
    }
}
