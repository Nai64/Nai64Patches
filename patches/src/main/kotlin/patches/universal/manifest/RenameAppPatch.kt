package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val renameAppPatch = resourcePatch(
    name = "Rename App",
    description = "Overrides the launcher label of the app",
    default = false,
) {
    val label by stringOption(
        title = "App name",
        default = "",
        key = "appLabel",
        description = "New launcher name. Leave empty to keep the original.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = label.orEmpty().trim()
        if (value.isEmpty()) {
            logger.warning("No label configured. No changes applied.")
            return@execute
        }

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                // A literal label overrides any @string reference on the application element.
                application.setAttributeNS(NS_ANDROID, "android:label", value)
                applied = true
            }
        }

        if (applied) {
            logger.info("Renamed app to \"$value\"")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
