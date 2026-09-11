package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

private val restrictedBackupAttributes = listOf(
    "android:dataExtractionRules",
    "android:fullBackupContent",
    "android:fullBackupOnly",
)

@Suppress("unused")
val preserveAppDataPatch = resourcePatch(
    name = "Preserve App Data",
    description = "Keeps app data when reinstalling by setting hasFragileUserData so Android offers to keep data.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull() ?: return@use

            application.setAttributeNS(NS_ANDROID, "android:hasFragileUserData", "true")
            application.setAttributeNS(NS_ANDROID, "android:allowBackup", "true")
            application.setAttributeNS(NS_ANDROID, "android:restoreAnyVersion", "true")

            var removedRules = 0
            for (attribute in restrictedBackupAttributes) {
                val localName = attribute.removePrefix("android:")
                if (application.hasAttributeNS(NS_ANDROID, localName)) {
                    application.removeAttributeNS(NS_ANDROID, localName)
                    removedRules++
                }
            }
            applied = true
        }

        if (applied) {
            logger.info("Enabled app-data preservation (keep-on-uninstall, backups, restore-any-version)")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
