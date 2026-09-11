package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val setProfileablePatch = resourcePatch(
    name = "Set Profileable",
    description =
        "Adds <profileable android:shell=\"true\"/> so profilers (Android Studio, simpleperf, " +
            "Perfetto) can attach to release builds for performance analysis",
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
                ?: return@use

            val existing = application.getElementsByTagName("profileable")
            if (existing.length > 0) {
                (existing.item(0) as? Element)?.setAttributeNS(NS_ANDROID, "android:shell", "true")
            } else {
                val profileable = manifest.createElement("profileable")
                profileable.setAttributeNS(NS_ANDROID, "android:shell", "true")
                application.appendChild(profileable)
            }
            applied = true
        }

        if (applied) {
            logger.info("Enabled profileable shell tracing")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
