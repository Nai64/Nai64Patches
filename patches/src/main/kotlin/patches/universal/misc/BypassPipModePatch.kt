package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassPipModePatch = bytecodePatch(
    name = "Bypass Picture-in-Picture Mode",
    description = "Makes Activity.isInPictureInPictureMode() report false so apps that change behavior in PiP keep their full UI.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bypass") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/app/Activity;" to mapOf("isInPictureInPictureMode" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched PiP-mode check(s) to false")
        else logger.warning("No PiP-mode checks found. No changes applied.")
    }
}
