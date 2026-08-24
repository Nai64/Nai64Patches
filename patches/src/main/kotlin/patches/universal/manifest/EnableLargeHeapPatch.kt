package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val enableLargeHeapPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Sets android:largeHeap so the app gets a bigger Java heap on devices that allow it, " +
            "reducing OutOfMemoryError crashes in memory-hungry games",
    default = false,
) {
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
