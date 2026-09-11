package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableMusicDetectionPatch = bytecodePatch(
    name = "Disable Music Detection",
    description = "Makes AudioManager.isMusicActive() report false so apps that react to playing music behave as if nothing is playing.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isMusicActive" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched music-active check(s) to false")
        else logger.warning("No music-active checks found. No changes applied.")
    }
}
