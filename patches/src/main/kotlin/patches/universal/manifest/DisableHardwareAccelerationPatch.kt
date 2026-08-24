package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val disableHardwareAccelerationPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Sets android:hardwareAccelerated=\"false\" on the <application> so the app renders on the software path (fixes some rendering/compat bugs).",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        document("AndroidManifest.xml").use { manifest ->
            val app = manifest.getElementsByTagName("application").item(0) as? Element
            if (app == null) {
                logger.warning("No <application> element found. No changes applied.")
                return@use
            }
            app.setAttributeNS(NS_ANDROID, "android:hardwareAccelerated", "false")
            logger.info("Disabled hardware acceleration")
        }
    }
}
