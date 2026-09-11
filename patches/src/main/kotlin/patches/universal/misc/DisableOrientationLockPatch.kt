package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableOrientationLockPatch = bytecodePatch(
    name = "Disable Orientation Lock",
    description = "Lets the app rotate with the device.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/Activity;", setOf("setRequestedOrientation"))
        if (patched > 0) logger.info("Neutralized $patched setRequestedOrientation() call(s)")
        else logger.warning("No setRequestedOrientation() calls found. No changes applied.")
    }
}
