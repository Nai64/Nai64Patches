package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val INTERNET_PERMISSION = "android.permission.INTERNET"

@Suppress("unused")
val ensureInternetPermissionPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Adds the INTERNET permission if it is missing. Useful after aggressive permission " +
            "stripping or for offline builds that still need web endpoints",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var added = false
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use

            var hasInternet = false
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in 0 until permissions.length) {
                val permission = permissions.item(i) as? Element ?: continue
                if (permission.getAttributeNS(NS_ANDROID, "name") == INTERNET_PERMISSION) {
                    hasInternet = true
                    break
                }
            }

            if (!hasInternet) {
                val permission = manifest.createElement("uses-permission")
                permission.setAttributeNS(NS_ANDROID, "android:name", INTERNET_PERMISSION)
                root.appendChild(permission)
                added = true
            }
        }

        if (added) {
            logger.info("Added missing INTERNET permission")
        } else {
            logger.warning("INTERNET permission already present. No changes applied.")
        }
    }
}
