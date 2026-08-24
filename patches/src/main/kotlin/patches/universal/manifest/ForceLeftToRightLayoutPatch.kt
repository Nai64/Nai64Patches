package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val forceLeftToRightLayoutPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:supportsRtl to false so the app always renders left-to-right, fixing " +
            "layouts that break under right-to-left system languages",
    default = false,
) {
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
