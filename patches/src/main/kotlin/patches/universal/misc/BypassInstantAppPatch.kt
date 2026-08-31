package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassInstantAppPatch = bytecodePatch(
    name = "Bypass Instant App",
    description = "Treats instant apps as full installs.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("isInstantApp" to "0x0")),
        )
        if (patched > 0) logger.info("Forced $patched instant-app check(s) to false")
        else logger.warning("No instant-app checks found. No changes applied.")
    }
}
