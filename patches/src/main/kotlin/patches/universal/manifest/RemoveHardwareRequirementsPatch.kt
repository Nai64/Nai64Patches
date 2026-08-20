package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val removeHardwareRequirementsPatch = resourcePatch(
    name = "Remove Hardware Requirements",
    description = "Remove uses-feature entries so the app installs on more devices",
    default = false,
) {
    val removeFeatures by stringOption(
        title = "Features to remove",
        default = "",
        key = "removeFeatures",
        description = "Comma-separated feature names to remove; leave empty to remove all uses-feature entries",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val toRemove = removeFeatures.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            val elements = manifest.getElementsByTagName("uses-feature")
            for (i in elements.length - 1 downTo 0) {
                val element = elements.item(i)
                val name = element.attributes.getNamedItem("android:name")?.nodeValue
                if (toRemove.isEmpty() || name in toRemove) {
                    element.parentNode?.removeChild(element)
                    removed++
                }
            }
        }

        if (removed == 0) {
            logger.info("No uses-feature entries found in the manifest")
        } else {
            logger.info("Removed $removed uses-feature entr${if (removed == 1) "y" else "ies"} from the manifest")
        }
    }
}