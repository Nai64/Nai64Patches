package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val removeNetworkSecurityConfigPatch = resourcePatch(
    name = "Remove Network Security Config",
    description =
        "Removes the app's network security config so certificate pinning and cleartext " +
            "restrictions fall back to platform defaults (useful for HTTPS inspection)",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
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
