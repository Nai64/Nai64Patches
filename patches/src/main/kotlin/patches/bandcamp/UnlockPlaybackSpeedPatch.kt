package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanGetterConst

@Suppress("unused")
val unlockPlaybackSpeedPatch = bytecodePatch(
    name = "Unlock Playback Speed",
    description = "Bandcamp: applies the playback-rate control to every track instead of podcasts only, so 1x, 1.5x and 2x speeds work for music too.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Lcom/bandcamp/fanapp/model/ModelController;",
            setOf("R1"),
            true,
        )
        if (patched > 0) logger.info("Forced $patched spoken-word check(s) so playback rate applies to all tracks")
        else logger.warning("No spoken-word checks found. No changes applied.")
    }
}
