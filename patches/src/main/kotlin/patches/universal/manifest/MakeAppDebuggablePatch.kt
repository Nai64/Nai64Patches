package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val makeAppDebuggablePatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Sets android:debuggable so debuggers and profiling tools can attach",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:debuggable", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:debuggable")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
