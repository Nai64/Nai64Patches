package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanGetterConst
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val disableOnboardingOverlaysPatch = bytecodePatch(
    name = "Disable Onboarding Overlays",
    description = "Bandcamp: skips the one-time onboarding overlays for the play queue, long-press downloads and public playlist tooltips.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val queuePatched = foldBooleanGetterConst(
            "Li9/n;",
            setOf("h"),
            true,
        )
        val downloadsPatched = foldBooleanGetterConst(
            "Lo6/w0;",
            setOf("g"),
            true,
        )
        val playlistsPatched = foldObjectGetterToNull(
            "Lx7/q0;",
            setOf("j"),
            "Lx7/q0\$c;",
        )
        val patched = queuePatched + downloadsPatched + playlistsPatched
        if (patched > 0) logger.info("Disabled $patched onboarding overlay gate(s)")
        else logger.warning("No onboarding overlay gates found. No changes applied.")
    }
}
