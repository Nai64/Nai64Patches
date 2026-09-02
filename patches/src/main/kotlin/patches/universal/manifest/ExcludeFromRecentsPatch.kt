package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

/**
 * Hides the app from the Android recent-tasks (overview) list by setting
 * android:excludeFromRecents="true" on the <application> element. Useful for
 * privacy when lending the device or keeping an app out of the switcher.
 */
@Suppress("unused")
val excludeFromRecentsPatch = resourcePatch(
    name = "Exclude From Recents",
    description = "Hides the app from recent apps.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            val application = root?.applicationOrNull()
            if (application == null) {
                logger.warning("No <application> element found. No changes applied.")
                return@execute
            }
            application.setAttributeNS(NS_ANDROID, "android:excludeFromRecents", "true")
            logger.info("Set android:excludeFromRecents=true on <application>")
        }
    }
}
