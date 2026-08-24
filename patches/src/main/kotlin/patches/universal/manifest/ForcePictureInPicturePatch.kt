package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

/**
 * Enables picture-in-picture for every activity by setting
 * android:supportsPictureInPicture="true", so videos can be popped into a
 * floating window.
 */
@Suppress("unused")
val forcePictureInPicturePatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Enables picture-in-picture for every activity by setting supportsPictureInPicture=true, so videos can be popped into a floating window.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var applied = 0
        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    element.setAttributeNS(NS_ANDROID, "android:supportsPictureInPicture", "true")
                    applied++
                }
            }
        }
        if (applied > 0) {
            logger.info("Enabled picture-in-picture on $applied activity component(s)")
        } else {
            logger.warning("No activities found in the manifest. No changes applied.")
        }
    }
}
