package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofStorageStatePatch = bytecodePatch(
    name = "Spoof Storage State",
    description = "Reports 'mounted' from Environment.getExternalStorageState() so apps that require mounted external storage run normally.",
    default = false,
) {
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
