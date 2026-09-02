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
        var patched = 0
        patched += foldBooleanGetterConst(
            "Lcom/bandcamp/fanapp/model/ModelController;",
            setOf("R1"),
            true,
        )
        // Genre.isSpokenWord is the underlying check R1 delegates to - also fold in case R1 is inlined
        patched += foldBooleanGetterConst(
            "Lcom/bandcamp/fanapp/model/Genre;",
            setOf("isSpokenWord"),
            true,
        )
        if (patched > 0) logger.info("Forced $patched spoken-word check(s) so playback rate applies to all tracks")
        else logger.warning("No spoken-word checks found. No changes applied.")
    }
}
