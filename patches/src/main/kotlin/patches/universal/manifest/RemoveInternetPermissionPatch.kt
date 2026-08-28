package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val INTERNET_PERMISSION = "android.permission.INTERNET"

@Suppress("unused")
val removeInternetPermissionPatch = resourcePatch(
    name = "Remove Internet Permission",
    description = "Removes internet permission for offline use. Apps needing internet will crash.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in nodes.length - 1 downTo 0) {
                    val el = nodes.item(i) as? Element ?: continue
                    val name = el.getAttributeNS(NS_ANDROID, "name").takeIf { it.isNotEmpty() }
                        ?: el.getAttribute("android:name").takeIf { it.isNotEmpty() }
                        ?: el.attributes.getNamedItem("android:name")?.nodeValue
                        ?: continue
                    if (name == INTERNET_PERMISSION) {
                        el.parentNode?.removeChild(el)
                        removed++
                    }
                }
            }
        }

        when {
            removed > 0 -> logger.info("Removed INTERNET permission ($removed entry(s))")
            else -> logger.info("INTERNET permission not declared. No changes applied.")
        }
    }
}
