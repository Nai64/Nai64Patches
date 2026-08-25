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
