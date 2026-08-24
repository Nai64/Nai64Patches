package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofAirplaneModePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Forces Settings.Global AIRPLANE_MODE_ON reads to 0 (off) so apps that change " +
            "behavior in airplane mode see a normal connection state.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldSettingsGetterConst(
            classes = setOf("Landroid/provider/Settings\$Global;"),
            keys = setOf("airplane_mode_on"),
        )

        if (patched > 0) {
            logger.info("Spoofed $patched airplane-mode read(s)")
        } else {
            logger.warning("No airplane-mode reads found. No changes applied.")
        }
    }
}
