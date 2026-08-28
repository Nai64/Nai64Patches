package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val hideAppOpsPatch = bytecodePatch(
    name = "Hide App Ops",
    description = "Makes app ops checks appear allowed so apps cannot probe permissions.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/AppOpsManager;" to mapOf(
                    "checkOp" to "0x0",
                    "checkOpNoThrow" to "0x0",
                    "noteOp" to "0x0",
                    "noteOpNoThrow" to "0x0",
                ),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Hid app ops at $patched call site(s)")
        else logger.warning("No AppOpsManager call sites found. No changes applied.")
    }
}
