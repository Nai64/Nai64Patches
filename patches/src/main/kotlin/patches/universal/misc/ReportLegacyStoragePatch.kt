package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val reportLegacyStoragePatch = bytecodePatch(
    name = "Report Legacy Storage",
    description =
        "Makes Environment.isExternalStorageLegacy always return true so apps behave as if " +
            "they got classic shared-storage access",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Misc") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/os/Environment;" to mapOf("isExternalStorageLegacy" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched storage-model check(s) to legacy")
        else logger.warning("No storage-model checks found. No changes applied.")
    }
}
