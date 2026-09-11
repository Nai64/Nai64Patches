package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassInstantAppPatch = bytecodePatch(
    name = "Bypass Instant App",
    description = "Treats instant apps as full installs.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Bypass") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("isInstantApp" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched instant-app check(s) to false")
        else logger.warning("No instant-app checks found. No changes applied.")
    }
}
