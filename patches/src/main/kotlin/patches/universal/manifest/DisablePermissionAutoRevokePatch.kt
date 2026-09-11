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
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
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
