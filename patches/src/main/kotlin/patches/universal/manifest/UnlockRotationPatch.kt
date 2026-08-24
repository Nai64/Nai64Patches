package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val unlockRotationPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Remove screenOrientation locks so the app rotates freely",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            val containers = mutableListOf<org.w3c.dom.Element>()
            val activities = manifest.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                containers.add(activities.item(i) as org.w3c.dom.Element)
            }
            val application = manifest.documentElement.getElementsByTagName("application")
            if (application.length > 0) {
                containers.add(application.item(0) as org.w3c.dom.Element)
            }

            for (element in containers) {
                if (element.attributes.getNamedItem("android:screenOrientation") != null) {
                    element.attributes.removeNamedItem("android:screenOrientation")
                    removed++
                }
            }
        }

        if (removed == 0) {
            logger.info("No screenOrientation locks found in the manifest")
        } else {
            logger.info("Removed $removed screenOrientation lock(s) from the manifest")
        }
    }
}
