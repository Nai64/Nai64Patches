package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceEmulatedStoragePatch = bytecodePatch(
    name = "Force Emulated Storage",
    description = "Makes Environment.isExternalStorageEmulated() report true so apps that expect emulated storage behave normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/os/Environment;" to mapOf("isExternalStorageEmulated" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched emulated-storage check(s) to true")
        else logger.warning("No emulated-storage checks found. No changes applied.")
    }
}
