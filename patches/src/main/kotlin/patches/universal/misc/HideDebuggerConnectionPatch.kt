package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideDebuggerConnectionPatch = bytecodePatch(
    name = "Hide Debugger Connection",
    description =
        "Makes Debug.isDebuggerConnected and waitingForDebugger always return false so apps " +
            "that refuse to run under a debugger keep working",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/os/Debug;" to mapOf(
                    "isDebuggerConnected" to "0x0",
                    "waitingForDebugger" to "0x0",
                ),
            ),
        )

        if (patched > 0) logger.info("Hid $patched debugger check(s)")
        else logger.warning("No debugger checks found. No changes applied.")
    }
}
