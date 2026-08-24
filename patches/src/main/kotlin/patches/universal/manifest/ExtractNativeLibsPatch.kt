package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val extractNativeLibsPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:extractNativeLibs so native libraries are extracted at install time. " +
            "Some mod loaders, hooks and older tools only work with extracted libs",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:extractNativeLibs", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:extractNativeLibs")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
