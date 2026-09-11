package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val keepDataOnUninstallPatch = resourcePatch(
    name = "Keep Data on Uninstall",
    description =
        "Sets android:hasFragileUserData so Android offers to keep the app's data when " +
            "uninstalling (the confirmation dialog appears on Android 10+)",
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
                application.setAttributeNS(NS_ANDROID, "android:hasFragileUserData", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:hasFragileUserData")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
