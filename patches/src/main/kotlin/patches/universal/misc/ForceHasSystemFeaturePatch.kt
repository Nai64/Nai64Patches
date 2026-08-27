package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceHasSystemFeaturePatch = bytecodePatch(
    name = "Force Has System Feature",
    description = "Makes PackageManager.hasSystemFeature always return true so feature-gated installs pass runtime checks",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/content/pm/PackageManager;" to mapOf("hasSystemFeature" to "0x1")),
        )
        if (patched > 0) logger.info("Forced hasSystemFeature true at $patched call site(s)")
        else logger.warning("No hasSystemFeature calls found. No changes applied.")
    }
}
