package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

private const val QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES"

@Suppress("unused")
val restorePackageVisibilityPatch = resourcePatch(
    name = "Restore Package Visibility",
    description = "Lets old apps see all installed apps on Android 11+.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var added = false

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in 0 until permissions.length) {
                val name = permissions.item(i)?.attributes
                    ?.getNamedItem("android:name")?.nodeValue
                if (name == QUERY_ALL_PACKAGES) return@use
            }

            val permission = manifest.createElement("uses-permission")
            permission.setAttributeNS(NS_ANDROID, "android:name", QUERY_ALL_PACKAGES)
            root.appendChild(permission)
            added = true
        }

        if (added) logger.info("Added QUERY_ALL_PACKAGES permission")
        else logger.info("QUERY_ALL_PACKAGES already declared. No changes applied.")
    }
}
