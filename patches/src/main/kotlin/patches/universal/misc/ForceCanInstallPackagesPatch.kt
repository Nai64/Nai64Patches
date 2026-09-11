package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceCanInstallPackagesPatch = bytecodePatch(
    name = "Force Can Install Packages",
    description = "Makes PackageManager.canRequestPackageInstalls() report true so sideload/unknown-source install prompts are treated as already granted.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("canRequestPackageInstalls" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched install-permission check(s) to true")
        else logger.warning("No install-permission checks found. No changes applied.")
    }
}
