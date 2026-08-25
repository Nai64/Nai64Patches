package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val splitManifestAttributes = setOf(
    "requiredSplitTypes",
    "splitTypes",
    "isSplitRequired",
    "isFeatureSplit",
    "split",
)

private val splitMetadataNames = setOf(
    "com.android.stamp.source",
    "com.android.stamp.type",
    "com.android.vending.splits",
    "com.android.vending.derived.apk.id",
    "com.android.dynamic.apk.fused.modules",
    "com.android.vending.splits.required",
)

@Suppress("unused")
val clearSplitMetadataPatch = resourcePatch(
    name = "Clearing Split Metadata",
    description = "Removes split-install manifest attributes and Play Store split metadata from the app manifest.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var removed = 0

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement

            for (attribute in splitManifestAttributes) {
                if (root.hasAttributeNS(NS_ANDROID, attribute)) {
                    root.removeAttributeNS(NS_ANDROID, attribute)
                    removed++
                }
                if (root.hasAttribute(attribute)) {
                    root.removeAttribute(attribute)
                    removed++
                }
                if (root.hasAttribute("android:$attribute")) {
                    root.removeAttribute("android:$attribute")
                    removed++
                }
            }

            val metadata = manifest.getElementsByTagName("meta-data")
            for (index in metadata.length - 1 downTo 0) {
                val element = metadata.item(index) as? Element ?: continue
                if (element.getAttributeNS(NS_ANDROID, "name") in splitMetadataNames) {
                    element.parentNode?.removeChild(element)
                    removed++
                }
            }
        }

        if (removed > 0) {
            logger.info("Removed $removed split-install manifest entr${if (removed == 1) "y" else "ies"}")
        } else {
            logger.warning("No split-install manifest metadata found. No changes applied.")
        }
    }
}
