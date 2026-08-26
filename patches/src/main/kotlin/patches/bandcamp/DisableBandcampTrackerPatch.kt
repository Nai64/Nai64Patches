package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.noOpVoidInvoke

@Suppress("unused")
val disableBandcampTrackerPatch = bytecodePatch(
    name = "Disable Bandcamp Tracker",
    description = "Bandcamp: disables the first-party accessibility tracker that pings /api/tracker.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke(
            "Lcom/bandcamp/shared/network/TrackerAPI;",
            setOf("f", "g"),
        )
        // StatsController ua.d is the main analytics pipeline (recordEvent etc.) — also void
        patched += noOpVoidInvoke(
            "Lua/d;",
            setOf("l", "m", "n", "o", "p", "q", "r", "s"),
        )
        if (patched > 0) logger.info("Disabled $patched tracker call(s)")
        else logger.warning("No tracker calls found. No changes applied.")
    }
}
