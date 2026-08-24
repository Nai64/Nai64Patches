package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.io.File
import java.util.logging.Logger
import org.w3c.dom.Element

private const val TRANSPARENT_DRAWABLE = "@drawable/nai64_transparent"

private val TRANSPARENT_XML = """
    <?xml version="1.0" encoding="utf-8"?>
    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <solid android:color="#00000000"/>
        <size android:width="1dp" android:height="1dp"/>
    </shape>
""".trimIndent() + "\n"

@Suppress("unused")
val removeAppIconPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Replaces the launcher icon with a transparent drawable",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val drawableDir = get("res/drawable", true)
        drawableDir.mkdirs()
        File(drawableDir, "nai64_transparent.xml").writeText(TRANSPARENT_XML)

        var replaced = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()

            fun Element.replaceIcons() {
                // The icon attribute is always set; roundIcon only when the app declares one.
                setAttributeNS(NS_ANDROID, "android:icon", TRANSPARENT_DRAWABLE)
                replaced++
                if (hasAttributeNS(NS_ANDROID, "roundIcon")) {
                    setAttributeNS(NS_ANDROID, "android:roundIcon", TRANSPARENT_DRAWABLE)
                    replaced++
                }
            }

            // The application-level icon covers every component that does not declare one.
            application?.replaceIcons()

            // Clear icons declared directly on activities and aliases as well.
            for (tag in listOf("activity", "activity-alias")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    (nodes.item(i) as? Element)?.replaceIcons()
                }
            }
        }

        if (replaced > 0) {
            logger.info("Replaced $replaced icon reference(s) with a transparent drawable")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
