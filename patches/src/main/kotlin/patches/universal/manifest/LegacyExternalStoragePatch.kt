package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"

@Suppress("unused")
val legacyExternalStoragePatch = resourcePatch(
    name = "Legacy External Storage",
    description =
        "Sets android:requestLegacyExternalStorage so the app keeps using the classic " +
            "shared-storage model instead of scoped storage (Android 10; ignored on 11+)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use
            val root = manifest.documentElement

            application.setAttributeNS(NS_ANDROID, "android:requestLegacyExternalStorage", "true")
            application.setAttributeNS(NS_ANDROID, "android:preserveLegacyExternalStorage", "true")
            applied = true

            // WRITE_EXTERNAL_STORAGE is required for the legacy model to grant access.
            var hasWritePermission = false
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in 0 until permissions.length) {
                val permission = permissions.item(i) as? Element ?: continue
                if (permission.getAttributeNS(NS_ANDROID, "name") == WRITE_EXTERNAL_STORAGE) {
                    hasWritePermission = true
                    break
                }
            }
            if (!hasWritePermission) {
                val permission = manifest.createElement("uses-permission")
                permission.setAttributeNS(NS_ANDROID, "android:name", WRITE_EXTERNAL_STORAGE)
                root.appendChild(permission)
                logger.info("Added missing WRITE_EXTERNAL_STORAGE permission")
            }
        }

        if (applied) {
            logger.info("Enabled legacy external storage")
        } else {
            logger.warning("No changes applied.")
        }
    }
}
