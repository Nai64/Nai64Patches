package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHeapPointerTaggingPatch = resourcePatch(
    name = "Disable Heap Pointer Tagging",
    description =
        "Sets android:allowNativeHeapPointerTagging to false so older native games that break " +
            "under Android 12+ tagged pointers keep running",
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
                application.setAttributeNS(
                    NS_ANDROID,
                    "android:allowNativeHeapPointerTagging",
                    "false",
                )
                applied = true
            }
        }

        if (applied) {
            logger.info("Disabled native heap pointer tagging")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
