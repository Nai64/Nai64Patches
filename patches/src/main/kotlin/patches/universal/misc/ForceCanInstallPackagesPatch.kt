package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceCanInstallPackagesPatch = bytecodePatch(
    name = "Force Can Install Packages",
    description = "Makes PackageManager.canRequestPackageInstalls() report true so sideload/unknown-source install prompts are treated as already granted.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("canRequestPackageInstalls" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched install-permission check(s) to true")
        else logger.warning("No install-permission checks found. No changes applied.")
    }
}
