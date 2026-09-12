package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val disableAudioEffectsPatch = bytecodePatch(
    name = "Disable Audio Effects",
    description = "Keeps equalizer, bass boost and other audio effects disabled.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/media/audiofx/BassBoost;" to mapOf("setEnabled" to "0x0"),
                "Landroid/media/audiofx/Virtualizer;" to mapOf("setEnabled" to "0x0"),
                "Landroid/media/audiofx/Equalizer;" to mapOf("setEnabled" to "0x0"),
                "Landroid/media/audiofx/LoudnessEnhancer;" to mapOf("setEnabled" to "0x0"),
                "Landroid/media/audiofx/PresetReverb;" to mapOf("setEnabled" to "0x0"),
                "Landroid/media/audiofx/EnvironmentalReverb;" to mapOf("setEnabled" to "0x0"),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Disabled $patched audio-effect call(s)")
        else logger.warning("No audio-effect calls found. No changes applied.")
    }
}
