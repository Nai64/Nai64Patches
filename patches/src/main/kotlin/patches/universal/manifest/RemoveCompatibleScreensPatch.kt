package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val removeCompatibleScreensPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Removes the <compatible-screens> block so the app is not restricted to a fixed set of screen sizes/densities.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var n = 0
        document("AndroidManifest.xml").use { manifest ->
            val screens = manifest.getElementsByTagName("compatible-screens")
            for (i in screens.length - 1 downTo 0) {
                val node = screens.item(i) ?: continue
                node.parentNode?.removeChild(node)
                n++
            }
        }
        if (n > 0) logger.info("Removed $n <compatible-screens> block(s)")
        else logger.warning("No <compatible-screens> elements found. No changes applied.")
    }
}
