package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val exportAllActivitiesPatch = resourcePatch(
    name = "Export All Activities",
    description =
        "Marks every activity and activity-alias as android:exported=true so other apps and " +
            "ADB can launch them. Unlike Repair Missing Component Export Flags, this intentionally " +
            "exports every activity even when it has no intent filter; use the repair patch when " +
            "you only need explicit exported values for filtered legacy components.",
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
