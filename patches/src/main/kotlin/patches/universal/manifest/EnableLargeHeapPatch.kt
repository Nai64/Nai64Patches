package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val enableLargeHeapPatch = resourcePatch(
    name = "Enable Large Heap",
    description =
        "Sets android:largeHeap so the app gets a bigger Java heap on devices that allow it, " +
            "reducing OutOfMemoryError crashes in memory-hungry games",
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
                application.setAttributeNS(NS_ANDROID, "android:largeHeap", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:largeHeap")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
