package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofWiredHeadsetPatch = bytecodePatch(
    name = "Spoof Wired Headset",
    description = "Makes AudioManager.isWiredHeadsetOn() report true so apps that require a wired headset think one is connected.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/media/AudioManager;" to mapOf("isWiredHeadsetOn" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched wired-headset check(s) to connected")
        else logger.warning("No wired-headset checks found. No changes applied.")
    }
}
