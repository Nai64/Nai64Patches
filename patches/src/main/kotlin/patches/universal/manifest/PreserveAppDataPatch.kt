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
    description =
        "Helps keep saves and app data when a patched build replaces an installed app. Sets " +
            "android:hasFragileUserData (Android offers to keep data on uninstall and restores it " +
            "when the same package is reinstalled), enables android:allowBackup, sets " +
            "android:restoreAnyVersion so a backup can be restored across versions, and removes " +
            "backup-exclusion rules that hide app data from device/cloud backups. Note: the patched " +
            "APK is re-signed, so it must replace the original via uninstall-then-install; keep the " +
            "package name unchanged (these patches never rename it) and the data returns",
    default = false,
) {
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
