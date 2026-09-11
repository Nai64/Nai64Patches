package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val relaxSharedLibrariesPatch = resourcePatch(
    name = "Relax Shared Libraries",
    description =
        "Marks every <uses-library> entry as optional so missing shared libraries no longer " +
            "block the app from installing on devices that lack them",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var relaxed = 0
        document("AndroidManifest.xml").use { manifest ->
            val libraries = manifest.getElementsByTagName("uses-library")
            for (i in 0 until libraries.length) {
                val library = libraries.item(i) as? Element ?: continue
                if (library.getAttributeNS(NS_ANDROID, "required") == "true") {
                    library.setAttributeNS(NS_ANDROID, "android:required", "false")
                    relaxed++
                }
            }
        }

        if (relaxed > 0) {
            logger.info("Made $relaxed shared library requirement(s) optional")
        } else {
            logger.warning("No required shared libraries found. No changes applied.")
        }
    }
}
