package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val disablePermissionAutoRevokePatch = resourcePatch(
    name = "Disable Permission Auto-Revoke",
    description =
        "Sets android:autoRevokePermissions to discouraged so Android stops resetting unused " +
            "permissions of apps you rarely open",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:autoRevokePermissions", "discouraged")
                applied = true
            }
        }

        if (applied) {
            logger.info("Disabled permission auto-revoke")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
