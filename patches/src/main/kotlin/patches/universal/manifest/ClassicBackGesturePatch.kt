package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val classicBackGesturePatch = resourcePatch(
    name = "Classic Back Gesture",
    description =
        "Sets android:enableOnBackInvokedCallback to false so apps that break under the " +
            "Android 13+ predictive back system keep the classic back behavior",
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
