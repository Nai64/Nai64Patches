package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val screenSizes = listOf(
    "smallScreens",
    "normalScreens",
    "largeScreens",
    "xlargeScreens",
)

@Suppress("unused")
val supportAllScreensPatch = resourcePatch(
    name = "Support All Screens",
    description =
        "Removes compatible-screens restrictions and marks every screen size supported so " +
            "the app installs on devices it would otherwise reject",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removedCompatible = 0
        var updatedSupports = false
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
                ?: return@use

            // Drop any hard device whitelist.
            val compatible = manifest.getElementsByTagName("compatible-screens")
            for (i in compatible.length - 1 downTo 0) {
                compatible.item(i)?.parentNode?.removeChild(compatible.item(i))
                removedCompatible++
            }

            val application = root.getElementsByTagName("application").item(0)

            val supports = manifest.getElementsByTagName("supports-screens")
            val supportsScreen = if (supports.length > 0) {
                supports.item(0) as? Element
            } else {
                val created = manifest.createElement("supports-screens")
                if (application != null) {
                    root.insertBefore(created, application)
                } else {
                    root.appendChild(created)
                }
                created
            }

            if (supportsScreen != null) {
                for (size in screenSizes) {
                    supportsScreen.setAttributeNS(NS_ANDROID, "android:$size", "true")
                }
                supportsScreen.setAttributeNS(NS_ANDROID, "android:anyDensity", "true")
                updatedSupports = true
            }
        }

        when {
            removedCompatible > 0 || updatedSupports ->
                logger.info(
                    "Removed $removedCompatible compatible-screens element(s), " +
                        "updated supports-screens: $updatedSupports",
                )

            else -> logger.warning("No screen configuration found. No changes applied.")
        }
    }
}
