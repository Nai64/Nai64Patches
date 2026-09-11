package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val disableAppBackupPatch = resourcePatch(
    name = "Disable App Backup",
    description = "Prevents the app from backing up its data.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        document("AndroidManifest.xml").use { manifest ->
            val app = manifest.getElementsByTagName("application").item(0) as? Element
            if (app == null) {
                logger.warning("No <application> element found. No changes applied.")
                return@use
            }
            app.setAttributeNS(NS_ANDROID, "android:allowBackup", "false")
            if (app.hasAttributeNS(NS_ANDROID, "backupAgent")) {
                app.removeAttributeNS(NS_ANDROID, "backupAgent")
            }
            if (app.hasAttributeNS(NS_ANDROID, "fullBackupContent")) {
                app.removeAttributeNS(NS_ANDROID, "fullBackupContent")
            }
            if (app.hasAttributeNS(NS_ANDROID, "fullBackupOnly")) {
                app.removeAttributeNS(NS_ANDROID, "fullBackupOnly")
            }
            logger.info("Disabled app backup (allowBackup=false)")
        }
    }
}
