package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val forceWideColorGamutPatch = bytecodePatch(
    name = "Force Wide Color Gamut",
    description = "Makes wide-gamut checks report true so apps render richer colors.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/view/Display;" to mapOf(
                    "isWideColorGamut" to "0x1",
                ),
                "Landroid/content/res/Configuration;" to mapOf(
                    "isScreenWideColorGamut" to "0x1",
                ),
            ),
        )
        if (patched > 0) logger.info("Forced $patched wide-gamut check(s) to true")
        else logger.warning("No wide-gamut checks found. No changes applied.")
    }
}
