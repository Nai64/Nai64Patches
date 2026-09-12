package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBoardAndHardwarePatch = bytecodePatch(
    name = "Spoof Board and Hardware",
    description = "Reports Pixel board and hardware names so apps cannot fingerprint the real device through them.",
    default = false,
) {
    category("Spoof")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStaticStringField(
            "Landroid/os/Build;",
            mapOf("BOARD" to "raven", "HARDWARE" to "raven"),
        )
        if (patched > 0) logger.info("Spoofed $patched board/hardware read(s)")
        else logger.warning("No board/hardware reads found. No changes applied.")
    }
}
