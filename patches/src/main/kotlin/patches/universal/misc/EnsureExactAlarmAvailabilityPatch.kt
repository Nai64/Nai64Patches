package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ensureExactAlarmAvailabilityPatch = bytecodePatch(
    name = "Ensure Exact Alarm Availability",
    description = "Makes exact alarms appear available so old alarm apps keep working.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/AlarmManager;" to mapOf("canScheduleExactAlarms" to "0x1"),
            ),
        )

        if (patched > 0) logger.info("Spoofed $patched exact-alarm availability check(s)")
        else logger.warning("No AlarmManager.canScheduleExactAlarms checks found. No changes applied.")
    }
}
