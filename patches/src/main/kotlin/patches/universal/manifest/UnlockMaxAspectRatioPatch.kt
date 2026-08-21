package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val unlockMaxAspectRatioPatch = resourcePatch(
    name = "Unlock Max Aspect Ratio",
    description =
        "Removes maxAspectRatio limits from the application, activities and metadata so the " +
            "app fills tall edge-to-edge screens instead of letterboxing",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removedAttrs = 0
        var removedMeta = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            if (application.hasAttributeNS(NS_ANDROID, "maxAspectRatio")) {
                application.removeAttributeNS(NS_ANDROID, "maxAspectRatio")
                removedAttrs++
            }

            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i) as? Element ?: continue
                    if (node.hasAttributeNS(NS_ANDROID, "maxAspectRatio")) {
                        node.removeAttributeNS(NS_ANDROID, "maxAspectRatio")
                        removedAttrs++
                    }
                }
            }

            val metadata = manifest.getElementsByTagName("meta-data")
            for (i in metadata.length - 1 downTo 0) {
                val meta = metadata.item(i) as? Element ?: continue
                if (meta.getAttributeNS(NS_ANDROID, "name") == "android.max_aspect") {
                    meta.parentNode?.removeChild(meta)
                    removedMeta++
                }
            }
        }

        when {
            removedAttrs > 0 || removedMeta > 0 ->
                logger.info(
                    "Removed $removedAttrs maxAspectRatio attribute(s) and $removedMeta metadata entry(ies)",
                )

            else -> logger.warning("No aspect ratio restrictions found. No changes applied.")
        }
    }
}
