package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val allowBackgroundActivityPatch = bytecodePatch(
    name = "Allow Background Activity",
    description =
        "Makes ActivityManager.isBackgroundRestricted always return false so apps ignore " +
            "background activity restrictions imposed by the system or user",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Allow") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/app/ActivityManager;" to mapOf("isBackgroundRestricted" to "0x0")),
        )

        if (patched > 0) logger.info("Forced $patched background-restriction check(s) to false")
        else logger.warning("No background-restriction checks found. No changes applied.")
    }
}
