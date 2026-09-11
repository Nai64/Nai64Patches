package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val removeCompatibleScreensPatch = resourcePatch(
    name = "Remove Compatible Screens",
    description = "Removes the <compatible-screens> block so the app is not restricted to a fixed set of screen sizes/densities.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
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
