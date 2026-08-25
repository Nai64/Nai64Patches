package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val treatScreenAsOnPatch = bytecodePatch(
    name = "Treat Screen as On",
    description =
        "Makes PowerManager.isInteractive always return true so apps keep playing audio or " +
            "progressing instead of pausing while the screen is off",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/os/PowerManager;" to mapOf("isInteractive" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched screen-state check(s) to on")
        else logger.warning("No screen-state checks found. No changes applied.")
    }
}
