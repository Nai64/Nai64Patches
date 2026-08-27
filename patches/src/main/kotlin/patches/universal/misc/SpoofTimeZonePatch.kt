package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofTimeZonePatch = bytecodePatch(
    name = "Spoof Time Zone",
    description = "Forces TimeZone.getDefault() to return a fixed timezone so region-locked apps see a constant zone (e.g. Europe/London for All4).",
    default = false,
) {
    val timeZoneId by stringOption(
        key = "timeZoneId",
        default = "UTC",
        title = "Time zone ID",
        description = "IANA time zone ID to spoof (e.g. UTC, Europe/London, America/New_York, Asia/Tokyo).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val tz = timeZoneId.orEmpty().trim().ifEmpty { "UTC" }
        // Validate is a known ID, fallback to UTC if invalid
        val zoneId = try {
            java.util.TimeZone.getTimeZone(tz).id.let { if (it == "GMT" && tz != "GMT") "UTC" else tz }
        } catch (_: Exception) {
            "UTC"
        }
        val patched = foldStringGetterConst(
            "Ljava/util/TimeZone;",
            setOf("getID"),
            zoneId,
        )
        if (patched > 0) logger.info("Spoofed time zone to $zoneId at $patched call site(s)")
        else logger.warning("No TimeZone.getID call sites found. No changes applied.")
    }
}
