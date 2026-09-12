package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanReturns
import java.util.logging.Logger

@Suppress("unused")
val disableHdrDetectionPatch = bytecodePatch(
    name = "Disable HDR Detection",
    description = "Makes HDR and wide-gamut checks report false so apps skip HDR rendering.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldBooleanReturns(
            mapOf(
                "Landroid/view/Display;" to mapOf(
                    "isHdr" to "0x0",
                    "isWideColorGamut" to "0x0",
                ),
                "Landroid/content/res/Configuration;" to mapOf(
                    "isScreenHdr" to "0x0",
                    "isScreenWideColorGamut" to "0x0",
                ),
            ),
        )
        if (patched > 0) logger.info("Disabled $patched HDR check(s)")
        else logger.warning("No HDR checks found. No changes applied.")
    }
}
