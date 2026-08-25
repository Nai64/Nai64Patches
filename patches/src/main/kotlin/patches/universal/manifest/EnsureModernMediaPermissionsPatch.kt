package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val modernMediaPermissions = listOf(
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
)

@Suppress("unused")
val ensureModernMediaPermissionsPatch = resourcePatch(
    name = "Ensure Modern Media Permissions",
    description =
        "Adds Android 13+ media permissions so older apps that can see some media but cannot " +
            "properly access photos, videos, or music can request the modern permissions. " +
            "Android still requires runtime approval, and this does not grant unrestricted access " +
            "to arbitrary files.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var added = 0

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use
            val declared = mutableSetOf<String>()
            for (tagName in listOf("uses-permission", "uses-permission-sdk-23")) {
                val permissions = manifest.getElementsByTagName(tagName)
                for (i in 0 until permissions.length) {
                    val permission = permissions.item(i) as? Element ?: continue
                    val name = permission.getAttributeNS(NS_ANDROID, "name")
                    if (name.isNotEmpty()) declared += name
                }
            }

            for (permissionName in modernMediaPermissions) {
                if (permissionName in declared) continue
                val permission = manifest.createElement("uses-permission")
                permission.setAttributeNS(NS_ANDROID, "android:name", permissionName)
                root.appendChild(permission)
                added++
                logger.info("Added missing $permissionName permission")
            }
        }

        if (added > 0) logger.info("Ensured $added modern media permission declaration(s)")
        else logger.info("Modern media permissions already declared. No changes applied.")
    }
}
