package patches.universal.display

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldNoArgIntGetter

@Suppress("unused")
val spoofUiModeNightPatch = bytecodePatch(
    name = "Spoof UiMode Night State",
    description = "Forces UiModeManager to report TYPE_NORMAL and night mode off so car/TV/night gates never trigger",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldNoArgIntGetter(
            definingClass = "Landroid/app/UiModeManager;",
            methodNames = setOf("getNightMode"),
            value = 1,
        )
        patched += foldNoArgIntGetter(
            definingClass = "Landroid/app/UiModeManager;",
            methodNames = setOf("getCurrentModeType"),
            value = 1,
        )
        if (patched > 0) logger.info("Spoofed UiMode $patched check(s)")
        else logger.warning("No UiMode checks found. No changes applied.")
    }
}
