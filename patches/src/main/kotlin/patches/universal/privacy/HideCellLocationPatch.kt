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
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
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
