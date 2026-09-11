package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofStorageStatePatch = bytecodePatch(
    name = "Spoof Storage State",
    description = "Reports 'mounted' from Environment.getExternalStorageState() so apps that require mounted external storage run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Spoof") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/os/Environment;",
            setOf("getExternalStorageState"),
            "mounted",
        )
        if (patched > 0) logger.info("Spoofed storage state at $patched call site(s)")
        else logger.warning("No getExternalStorageState call sites found. No changes applied.")
    }
}
