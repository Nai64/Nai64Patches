package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableOrientationLockPatch = bytecodePatch(
    name = "Disable Orientation Lock",
    description = "Lets the app rotate with the device.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke("Landroid/app/Activity;", setOf("setRequestedOrientation"))
        if (patched > 0) logger.info("Neutralized $patched setRequestedOrientation() call(s)")
        else logger.warning("No setRequestedOrientation() calls found. No changes applied.")
    }
}
