package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val makeAppDebuggablePatch = resourcePatch(
    name = "Make App Debuggable",
    description = "Sets android:debuggable so debuggers and profiling tools can attach",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
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
