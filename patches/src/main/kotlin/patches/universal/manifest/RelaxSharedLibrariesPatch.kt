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
