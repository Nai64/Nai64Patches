package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val removeNetworkSecurityConfigPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Removes the app's network security config so certificate pinning and cleartext " +
            "restrictions fall back to platform defaults (useful for HTTPS inspection)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            if (application.hasAttributeNS(NS_ANDROID, "networkSecurityConfig")) {
                application.removeAttributeNS(NS_ANDROID, "networkSecurityConfig")
                removed = true
            }
        }

        if (removed) {
            logger.info("Removed the network security config reference")
        } else {
            logger.warning("No network security config found. No changes applied.")
        }
    }
}
