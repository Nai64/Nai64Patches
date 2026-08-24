package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val classicBackGesturePatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:enableOnBackInvokedCallback to false so apps that break under the " +
            "Android 13+ predictive back system keep the classic back behavior",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:enableOnBackInvokedCallback", "false")
                applied = true
            }
        }

        if (applied) {
            logger.info("Opted out of the predictive back system")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
