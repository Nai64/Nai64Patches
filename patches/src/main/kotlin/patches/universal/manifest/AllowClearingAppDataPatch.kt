package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val allowClearingAppDataPatch = resourcePatch(
    name = "Allow Clearing App Data",
    description =
        "Sets android:allowClearUserData so the system's Clear Data button stays available " +
            "even for apps that try to disable it",
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
                application.setAttributeNS(NS_ANDROID, "android:allowClearUserData", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:allowClearUserData")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
