package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val allowCleartextTrafficPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:usesCleartextTraffic and drops any network security config so plain " +
            "HTTP requests are allowed (useful for debugging through local proxies)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        var droppedConfig = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            application.setAttributeNS(NS_ANDROID, "android:usesCleartextTraffic", "true")
            applied = true

            // A network security config silently overrides usesCleartextTraffic.
            if (application.hasAttributeNS(NS_ANDROID, "networkSecurityConfig")) {
                application.removeAttributeNS(NS_ANDROID, "networkSecurityConfig")
                droppedConfig = true
            }
        }

        when {
            applied && droppedConfig -> logger.info("Allowed cleartext traffic and removed the network security config")
            applied -> logger.info("Allowed cleartext traffic")
            else -> logger.warning("No <application> element found. No changes applied.")
        }
    }
}
