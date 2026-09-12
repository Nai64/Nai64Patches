package patches.universal.graphics

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private const val NS_ANDROID_GRAPHICS = "http://schemas.android.com/apk/res/android"

@Suppress("unused")
val useDisplayCutoutAreaPatch = resourcePatch(
    name = "Use Display Cutout Area",
    description = "Renders inside the notch and punch-hole area for fullscreen games.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var applied = 0
        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    element.setAttributeNS(NS_ANDROID_GRAPHICS, "android:windowLayoutInDisplayCutoutMode", "shortEdges")
                    applied++
                }
            }
        }
        if (applied > 0) logger.info("Enabled cutout area on $applied activity component(s)")
        else logger.warning("No activities found in the manifest. No changes applied.")
    }
}
