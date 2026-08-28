package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldObjectGetterToNull
import java.util.logging.Logger

@Suppress("unused")
val hideCellLocationPatch = bytecodePatch(
    name = "Hide Cell Location",
    description = "Makes cell location appear unavailable so apps cannot read your tower location.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Landroid/telephony/TelephonyManager;",
            setOf("getCellLocation"),
            "Landroid/telephony/CellLocation;",
        )
        if (patched > 0) logger.info("Hid cell location at $patched call site(s)")
        else logger.warning("No getCellLocation call sites found. No changes applied.")
    }
}
