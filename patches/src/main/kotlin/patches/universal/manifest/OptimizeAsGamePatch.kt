package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val optimizeAsGamePatch = resourcePatch(
    name = "Optimize as Game",
    description =
        "Sets android:appCategory to game so Android applies game mode, battery and " +
            "screenshot optimizations tuned for games",
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
                application.setAttributeNS(NS_ANDROID, "android:appCategory", "game")
                applied = true
            }
        }

        if (applied) {
            logger.info("Set app category to game")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
