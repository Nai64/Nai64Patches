package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableFixedVolumePatch = bytecodePatch(
    name = "Disable Fixed Volume",
    description = "Makes AudioManager.isVolumeFixed() report false so volume keys and stream changes work even when an app pins the volume.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isVolumeFixed" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched fixed-volume check(s) to false")
        else logger.warning("No fixed-volume checks found. No changes applied.")
    }
}
