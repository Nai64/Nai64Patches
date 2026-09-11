package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val removePlayCountLimitPatch = bytecodePatch(
    name = "Remove Play Count Limit",
    description = "Bandcamp: removes the free streaming play limit on unowned tralbums, so full tracks keep playing and the purchase nag never appears.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bandcamp") } catch (_: NoSuchMethodError) {}
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Lcom/bandcamp/fanapp/model/ModelController\$b2;" to mapOf(
                    "a" to "0x0",
                    "b" to "0x0",
                ),
            ),
        )
        if (patched > 0) logger.info("Disabled $patched play-limit getter call(s)")
        else logger.warning("No play-limit getters found. No changes applied.")
    }
}
