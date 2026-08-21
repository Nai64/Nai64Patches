package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val adServicesPermissions = setOf(
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
)

private const val AD_ID_PERMISSION = "com.google.android.gms.permission.AD_ID"

@Suppress("unused")
val removeAdServicesEntriesPatch = resourcePatch(
    name = "Remove Ad Services Entries",
    description =
        "Strips Privacy Sandbox (AdServices) permissions, the ad ID permission and their " +
            "config metadata from the manifest",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            // Permission entries.
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in permissions.length - 1 downTo 0) {
                val permission = permissions.item(i) as? Element ?: continue
                val name = permission.getAttributeNS(NS_ANDROID, "name")
                if (name in adServicesPermissions || name == AD_ID_PERMISSION) {
                    permission.parentNode?.removeChild(permission)
                    removed++
                }
            }

            // AdServices config declared as <property> or <meta-data>.
            for (tag in listOf("property", "meta-data")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (i in nodes.length - 1 downTo 0) {
                    val node = nodes.item(i) as? Element ?: continue
                    val name = node.getAttributeNS(NS_ANDROID, "name")
                    if (name.startsWith("android.adservices.")) {
                        node.parentNode?.removeChild(node)
                        removed++
                    }
                }
            }
        }

        if (removed > 0) {
            logger.info("Removed $removed AdServices/ad-id manifest entry(ies)")
        } else {
            logger.warning("No AdServices entries found. No changes applied.")
        }
    }
}
