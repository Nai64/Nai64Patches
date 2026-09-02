package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val forceOrientationPatch = resourcePatch(
    name = "Force Landscape / Portrait",
    description = "Force the app to a fixed screen orientation",
    default = false,
) {
    val orientation by stringOption(
        title = "Orientation",
        default = "landscape",
        key = "orientation",
        description = "Force a fixed screen orientation on all activities",
        values = linkedMapOf(
            "Landscape" to "landscape",
            "Portrait" to "portrait",
            "Sensor Landscape" to "sensorLandscape",
            "Sensor Portrait" to "sensorPortrait",
            "User Landscape" to "userLandscape",
            "User Portrait" to "userPortrait",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = orientation.orEmpty().trim()
        val valid = setOf("landscape", "portrait", "sensorLandscape", "sensorPortrait", "userLandscape", "userPortrait")
        if (value !in valid) {
            logger.warning("Invalid orientation \"$value\". No changes applied.")
            return@execute
        }

        var patched = 0
        document("AndroidManifest.xml").use { manifest ->
            val containers = mutableListOf<org.w3c.dom.Element>()
            val activities = manifest.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                containers.add(activities.item(i) as org.w3c.dom.Element)
            }
            val depsActivities = manifest.getElementsByTagName("activity-alias")
            for (i in 0 until depsActivities.length) {
                containers.add(depsActivities.item(i) as org.w3c.dom.Element)
            }
            val application = manifest.documentElement.getElementsByTagName("application")
            if (application.length > 0) {
                containers.add(application.item(0) as org.w3c.dom.Element)
            }

            for (element in containers) {
                element.setAttribute("android:screenOrientation", value)
                // Foldable/tablet fix (#61): make large inner display respect orientation and allow resize
                element.setAttributeNS("http://schemas.android.com/apk/res/android", "android:resizeableActivity", "true")
                val existingConfig = element.getAttributeNS("http://schemas.android.com/apk/res/android", "configChanges")
                val needed = listOf("orientation", "screenSize", "smallestScreenSize", "screenLayout", "density", "layoutDirection")
                val merged = if (existingConfig.isNullOrEmpty()) {
                    needed.joinToString("|")
                } else {
                    val parts = existingConfig.split("|").map { it.trim() }.toMutableSet()
                    parts.addAll(needed)
                    parts.joinToString("|")
                }
                element.setAttributeNS("http://schemas.android.com/apk/res/android", "android:configChanges", merged)
                patched++
            }
            // Ensure supports-screens allows large/xlarge for foldable inner display
            var supportsScreens = manifest.getElementsByTagName("supports-screens").item(0) as? org.w3c.dom.Element
            if (supportsScreens == null) {
                supportsScreens = manifest.createElement("supports-screens")
                manifest.documentElement.appendChild(supportsScreens)
            }
            supportsScreens.setAttributeNS("http://schemas.android.com/apk/res/android", "android:smallScreens", "true")
            supportsScreens.setAttributeNS("http://schemas.android.com/apk/res/android", "android:normalScreens", "true")
            supportsScreens.setAttributeNS("http://schemas.android.com/apk/res/android", "android:largeScreens", "true")
            supportsScreens.setAttributeNS("http://schemas.android.com/apk/res/android", "android:xlargeScreens", "true")
            supportsScreens.setAttributeNS("http://schemas.android.com/apk/res/android", "android:requiresSmallestWidthDp", "0")
        }

        if (patched == 0) {
            logger.warning("No activities found in the manifest. No changes applied.")
        } else {
            logger.info("Forced $value on $patched element(s)")
        }
    }
}