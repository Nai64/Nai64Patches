package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
private const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"

/**
 * Adds the legacy storage declarations used by older applications that can read
 * media through modern providers but fail when browsing ordinary files.
 */
@Suppress("unused")
val ensureStoragePermissionsPatch = resourcePatch(
    name = "Ensure Storage Permissions",
    description =
        "Adds READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE for older apps that can see " +
            "photos, videos, or music but cannot access ordinary files on modern Android ROMs. " +
            "The permissions still require Android to grant them at runtime and have limited " +
            "effect for apps targeting Android 13+.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val requiredPermissions = setOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        var added = 0

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use
            val declaredPermissions = mutableSetOf<String>()

            for (tagName in listOf("uses-permission", "uses-permission-sdk-23")) {
                val permissions = manifest.getElementsByTagName(tagName)
                for (i in 0 until permissions.length) {
                    val permission = permissions.item(i) as? Element ?: continue
                    val name = permission.getAttributeNS(NS_ANDROID, "name")
                    if (name.isNotEmpty()) declaredPermissions += name
                }
            }

            for (permissionName in requiredPermissions) {
                if (permissionName in declaredPermissions) continue

                val permission = manifest.createElement("uses-permission")
                permission.setAttributeNS(NS_ANDROID, "android:name", permissionName)
                root.appendChild(permission)
                added++
                logger.info("Added missing $permissionName permission")
            }
        }

        if (added > 0) {
            logger.info("Ensured $added legacy storage permission declaration(s)")
        } else {
            logger.info("Legacy storage permissions already declared. No changes applied.")
        }
    }
}
