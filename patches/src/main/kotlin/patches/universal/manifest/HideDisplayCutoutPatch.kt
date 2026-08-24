package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

/**
 * Forces android:windowLayoutInDisplayCutoutMode="never" on every activity so
 * notched or punch-hole screens use the full display without a black bar.
 */
@Suppress("unused")
val hideDisplayCutoutPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Forces windowLayoutInDisplayCutoutMode=never on every activity so notched or punch-hole screens use the full display without a black bar.",
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
                    element.setAttributeNS(NS_ANDROID, "android:windowLayoutInDisplayCutoutMode", "never")
                    applied++
                }
            }
        }
        if (applied > 0) {
            logger.info("Hid display cutout on $applied activity component(s)")
        } else {
            logger.warning("No activities found in the manifest. No changes applied.")
        }
    }
}
