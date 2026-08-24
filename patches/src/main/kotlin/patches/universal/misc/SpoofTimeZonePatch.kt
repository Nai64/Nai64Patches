package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofTimeZonePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces TimeZone.getDefault().getID() to return UTC so region- or time-zone-locked apps see a constant zone.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Ljava/util/TimeZone;",
            setOf("getID"),
            "UTC",
        )
        if (patched > 0) logger.info("Spoofed time zone at $patched call site(s)")
        else logger.warning("No TimeZone.getID call sites found. No changes applied.")
    }
}
