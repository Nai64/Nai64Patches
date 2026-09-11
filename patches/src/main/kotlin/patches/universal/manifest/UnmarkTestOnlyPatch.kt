package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val unmarkTestOnlyPatch = resourcePatch(
    name = "Unmark Test Only",
    description =
        "Removes android:testOnly from the manifest so builds marked as test-only can be " +
            "installed and launched like normal apps",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            if (application.hasAttributeNS(NS_ANDROID, "testOnly")) {
                application.removeAttributeNS(NS_ANDROID, "testOnly")
                removed = true
            }
        }

        if (removed) {
            logger.info("Removed the testOnly flag")
        } else {
            logger.warning("No testOnly flag found. No changes applied.")
        }
    }
}
