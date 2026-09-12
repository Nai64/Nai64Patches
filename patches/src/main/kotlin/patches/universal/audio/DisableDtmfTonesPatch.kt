package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val disableDtmfTonesPatch = bytecodePatch(
    name = "Disable DTMF Tones",
    description = "Makes dialer tone playback report failure so keypad tones stay silent.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/ToneGenerator;" to mapOf("startTone" to "0x0")),
        )
        if (patched > 0) logger.info("Disabled $patched DTMF tone call(s)")
        else logger.warning("No DTMF tone calls found. No changes applied.")
    }
}
