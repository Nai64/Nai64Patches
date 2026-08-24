package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val exportAllActivitiesPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Marks every activity as android:exported so other apps and ADB can launch them",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var exported = 0
        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    element.setAttributeNS(NS_ANDROID, "android:exported", "true")
                    exported++
                }
            }
        }

        if (exported > 0) {
            logger.info("Exported $exported activity component(s)")
        } else {
            logger.warning("No activities found in the manifest. No changes applied.")
        }
    }
}
