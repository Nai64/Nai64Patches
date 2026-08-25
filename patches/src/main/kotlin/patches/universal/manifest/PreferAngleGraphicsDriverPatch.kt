package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val ANGLE_DRIVER_METADATA = "com.android.graphics.driver.prefer_angle"

@Suppress("unused")
val preferAngleGraphicsDriverPatch = resourcePatch(
    name = "Prefer ANGLE Graphics Driver",
    description =
        "Requests ANGLE as the OpenGL ES driver so compatible modern Android devices may run " +
            "OpenGL ES through ANGLE for improved graphics compatibility. This is only a driver " +
            "preference, is not guaranteed, does not convert Vulkan apps to OpenGL, and may cause " +
            "rendering issues on unsupported devices.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var applied = false

        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull() ?: return@use
            val metadata = application.getElementsByTagName("meta-data")
            var angleMetadata: Element? = null
            for (i in 0 until metadata.length) {
                val element = metadata.item(i) as? Element ?: continue
                if (element.getAttributeNS(NS_ANDROID, "name") == ANGLE_DRIVER_METADATA) {
                    angleMetadata = element
                    break
                }
            }

            if (angleMetadata != null) {
                val currentValue = angleMetadata.getAttributeNS(NS_ANDROID, "value")
                if (currentValue.equals("true", ignoreCase = true)) {
                    logger.info("ANGLE graphics driver preference already enabled. No changes applied.")
                    return@use
                }

                angleMetadata.setAttributeNS(NS_ANDROID, "android:value", "true")
                applied = true
                logger.info("Changed existing ANGLE graphics driver preference to true")
            } else {
                val element = manifest.createElement("meta-data")
                element.setAttributeNS(NS_ANDROID, "android:name", ANGLE_DRIVER_METADATA)
                element.setAttributeNS(NS_ANDROID, "android:value", "true")
                application.appendChild(element)
                applied = true
                logger.info("Added ANGLE graphics driver preference")
            }
        }

        if (!applied) logger.warning("No <application> element found. No changes applied.")
    }
}
