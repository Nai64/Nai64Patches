package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val forceHardwareAccelerationPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:hardwareAccelerated so every activity renders with GPU acceleration, " +
            "even when the app disables it globally",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        var enabledActivities = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:hardwareAccelerated", "true")
                applied = true
            }

            // Clear per-activity opt-outs as well.
            val activities = manifest.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (activity.getAttributeNS(NS_ANDROID, "hardwareAccelerated") == "false") {
                    activity.setAttributeNS(NS_ANDROID, "android:hardwareAccelerated", "true")
                    enabledActivities++
                }
            }
        }

        if (applied) {
            logger.info("Enabled hardware acceleration ($enabledActivities activity override(s) cleared)")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
