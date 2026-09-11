package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val forceLeftToRightLayoutPatch = resourcePatch(
    name = "Force Left-to-Right Layout",
    description =
        "Sets android:supportsRtl to false so the app always renders left-to-right, fixing " +
            "layouts that break under right-to-left system languages",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:supportsRtl", "false")
                applied = true
            }
        }

        if (applied) {
            logger.info("Forced left-to-right layout")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
