package patches.universal.manifest

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val forceResizableActivityPatch = resourcePatch(
    name = "Force Resizable Activity",
    description =
        "Sets android:resizeableActivity and supportsFreeformWindowManagement so the " +
            "app can run in split-screen, free-form windows, DeX, and Chromebooks",
    default = false,
) {
    val freeform by booleanOption(
        title = "Enable freeform",
        default = true,
        key = "freeform",
        description = "Also set supportsFreeformWindowManagement for true free-form.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        var removedFeatures = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use
            val root = manifest.documentElement

            application.setAttributeNS(NS_ANDROID, "android:resizeableActivity", "true")
            if (freeform == true) {
                application.setAttributeNS(NS_ANDROID, "android:supportsFreeformWindowManagement", "true")
                // Also apply to all activities for completeness
                val activities = manifest.getElementsByTagName("activity")
                for (i in 0 until activities.length) {
                    (activities.item(i) as? Element)?.setAttributeNS(NS_ANDROID, "android:resizeableActivity", "true")
                    (activities.item(i) as? Element)?.setAttributeNS(NS_ANDROID, "android:supportsFreeformWindowManagement", "true")
                }
            }
            applied = true

            val features = manifest.getElementsByTagName("uses-feature")
            for (i in features.length - 1 downTo 0) {
                val feature = features.item(i) as? Element ?: continue
                if (feature.getAttributeNS(NS_ANDROID, "name") == "android.hardware.touchscreen") {
                    feature.parentNode?.removeChild(feature)
                    removedFeatures++
                }
            }
        }

        when {
            applied && removedFeatures > 0 ->
                logger.info("Enabled resizable activity and removed $removedFeatures touchscreen requirement(s)")

            applied -> logger.info("Enabled resizable activity")
            else -> logger.warning("No <application> element found. No changes applied.")
        }
    }
}
