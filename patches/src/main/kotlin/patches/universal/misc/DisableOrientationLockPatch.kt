package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableOrientationLockPatch = bytecodePatch(
    name = "Disable Orientation Lock",
    description = "No-ops Activity.setRequestedOrientation(...) so the app cannot force a fixed screen orientation; the device rotation is respected.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/Activity;", setOf("setRequestedOrientation"))
        if (patched > 0) logger.info("Neutralized $patched setRequestedOrientation() call(s)")
        else logger.warning("No setRequestedOrientation() calls found. No changes applied.")
    }
}
