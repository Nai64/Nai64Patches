package patches.universal.audio

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldNoArgIntGetter
import java.util.logging.Logger

@Suppress("unused")
val spoofHighSampleRatePatch = bytecodePatch(
    name = "Spoof High Sample Rate",
    description = "Reports 48000 Hz sample rate so apps unlock high-quality audio paths.",
    default = false,
) {
    category("Audio")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/media/AudioTrack;",
            setOf("getSampleRate"),
            48000,
        ) + foldNoArgIntGetter(
            "Landroid/media/AudioRecord;",
            setOf("getSampleRate"),
            48000,
        )
        if (patched > 0) logger.info("Spoofed $patched sample-rate check(s) to 48000 Hz")
        else logger.warning("No sample-rate checks found. No changes applied.")
    }
}
