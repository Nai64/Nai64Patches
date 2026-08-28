package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

/**
 * Reports a fake device serial through android.os.Build.getSerial() so apps
 * that fingerprint by serial number see a constant value.
 */
@Suppress("unused")
val spoofBuildSerialPatch = bytecodePatch(
    name = "Spoof Build Serial",
    description = "Reports a chosen device serial through Build.getSerial() so apps that fingerprint by serial number see a constant value.",
    default = false,
) {
    val serial by stringOption(
        title = "Serial",
        default = "unknown",
        key = "buildSerial",
        description = "Serial to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/os/Build;",
            setOf("getSerial"),
            serial ?: "unknown",
        )
        if (patched > 0) {
            logger.info("Spoofed build serial at $patched call site(s)")
        } else {
            logger.warning("No Build.getSerial call sites found. No changes applied.")
        }
    }
}
