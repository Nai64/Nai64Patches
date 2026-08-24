package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val enableHiddenSettingsPatch = bytecodePatch(
    name = "Enable Hidden Settings",
    description = "Bandcamp: turns on hidden feature flags - audio-cache indicators, podcast player controls and public-playlist track visibility.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Ln8/o;" to mapOf(
                    "B" to "0x1",
                    "i0" to "0x1",
                    "j0" to "0x1",
                ),
            ),
        )
        if (patched > 0) logger.info("Forced $patched hidden-feature flag getter(s) to true")
        else logger.warning("No hidden-feature flag getters found. No changes applied.")
    }
}
