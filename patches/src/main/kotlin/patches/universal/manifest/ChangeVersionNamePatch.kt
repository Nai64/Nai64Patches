package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val changeVersionNamePatch = resourcePatch(
    name = "Change Version Name",
    description = "Overrides the android:versionName declared in the manifest",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    val versionName by stringOption(
        title = "Version name",
        default = "",
        key = "versionName",
        description = "New version name (e.g. 2.5.1). Leave empty to keep the original.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = versionName.orEmpty().trim()
        if (value.isEmpty()) {
            logger.warning("No valid version name configured. No changes applied.")
            return@execute
        }

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            if (root != null && root.tagName == "manifest") {
                root.setAttributeNS(NS_ANDROID, "android:versionName", value)
                applied = true
            }
        }

        if (applied) {
            logger.info("Changed version name to \"$value\"")
        } else {
            logger.warning("No <manifest> root element found. No changes applied.")
        }
    }
}
