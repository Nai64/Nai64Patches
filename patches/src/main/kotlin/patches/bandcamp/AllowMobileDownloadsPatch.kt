package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val allowMobileDownloadsPatch = bytecodePatch(
    name = "Allow Mobile Downloads",
    description = "Bandcamp: allows audio cache downloads over mobile data without the confirmation dialog.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Lcom/bandcamp/fanapp/player/cache/AudioCache;" to mapOf(
                    "h0" to "0x1",
                ),
            ),
        )
        if (patched > 0) logger.info("Allowed $patched mobile-download check(s)")
        else logger.warning("No mobile-download checks found. No changes applied.")
    }
}
