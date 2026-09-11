package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val grantAllFilesAccessPatch = bytecodePatch(
    name = "Grant All-Files Access",
    description =
        "Makes Environment.isExternalStorageManager always return true so apps accept the " +
            "storage permission state without the special Android 11+ prompt",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Permissions") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/os/Environment;" to mapOf("isExternalStorageManager" to "0x1"),
            ),
        )

        if (patched > 0) logger.info("Forced $patched storage-access check(s) to true")
        else logger.warning("No all-files-access checks found. No changes applied.")
    }
}
