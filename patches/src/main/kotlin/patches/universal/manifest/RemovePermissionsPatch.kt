package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val removePermissionsPatch = resourcePatch(
    name = "Remove Permissions",
    description = "Remove permissions from the app manifest",
    default = false,
) {
    val removePermissions by stringOption(
        title = "Permissions to remove",
        default = "android.permission.SEND_SMS,android.permission.RECEIVE_SMS,android.permission.READ_SMS,android.permission.RECORD_AUDIO,android.permission.CAMERA",
        key = "removePermissions",
        description = "Comma-separated permission names to remove from the manifest",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val toRemove = removePermissions.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (toRemove.isEmpty()) {
            logger.warning("No permissions to remove. No changes applied.")
            return@execute
        }

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            val elements = manifest.getElementsByTagName("uses-permission")
            for (i in elements.length - 1 downTo 0) {
                val element = elements.item(i)
                val name = element.attributes.getNamedItem("android:name")?.nodeValue ?: continue
                if (name in toRemove) {
                    element.parentNode?.removeChild(element)
                    removed++
                }
            }
        }

        if (removed == 0) {
            logger.info("No matching permissions found in the manifest")
        } else {
            logger.info("Removed $removed permission(s) from the manifest")
        }
    }
}