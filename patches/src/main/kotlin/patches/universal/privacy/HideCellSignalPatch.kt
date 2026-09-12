package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideCellSignalPatch = bytecodePatch(
    name = "Hide Cell Signal",
    description = "Reports no cell signal strength so apps cannot read your tower signal or track location changes",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Landroid/telephony/TelephonyManager;",
            setOf("getSignalStrength"),
            "Landroid/telephony/SignalStrength;",
        )
        if (patched > 0) logger.info("Hid cell signal at $patched call site(s)")
        else logger.warning("No cell signal calls found. No changes applied.")
    }
}
