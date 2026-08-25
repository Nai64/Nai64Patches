package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofRingerModePatch = bytecodePatch(
    name = "Spoof Ringer Mode",
    description =
        "Forces AudioManager.getRingerMode() to report RINGER_MODE_NORMAL (2) so apps " +
            "cannot detect silent or vibrate mode.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldNoArgIntGetter(
            definingClass = "Landroid/media/AudioManager;",
            methodNames = setOf("getRingerMode"),
            value = 2, // AudioManager.RINGER_MODE_NORMAL
        )

        if (patched > 0) {
            logger.info("Spoofed $patched ringer-mode read(s)")
        } else {
            logger.warning("No ringer-mode reads found. No changes applied.")
        }
    }
}
