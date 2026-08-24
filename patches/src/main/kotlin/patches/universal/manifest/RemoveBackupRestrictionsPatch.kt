package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

private val restrictedBackupAttributes = listOf(
    "android:dataExtractionRules",
    "android:fullBackupContent",
    "android:fullBackupOnly",
)

@Suppress("unused")
val removeBackupRestrictionsPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:allowBackup and removes data extraction / full backup rules that " +
            "exclude app data from device transfers and cloud backups",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var enabledBackup = false
        var removedRules = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            application.setAttributeNS(NS_ANDROID, "android:allowBackup", "true")
            enabledBackup = true

            for (attribute in restrictedBackupAttributes) {
                val localName = attribute.removePrefix("android:")
                if (application.hasAttributeNS(NS_ANDROID, localName)) {
                    application.removeAttributeNS(NS_ANDROID, localName)
                    removedRules++
                }
            }
        }

        when {
            enabledBackup && removedRules > 0 ->
                logger.info("Enabled backups and removed $removedRules backup rule(s)")

            enabledBackup -> logger.info("Enabled backups")
            else -> logger.warning("No <application> element found. No changes applied.")
        }
    }
}
