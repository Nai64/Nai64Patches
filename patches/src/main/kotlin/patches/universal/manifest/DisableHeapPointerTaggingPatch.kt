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
