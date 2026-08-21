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
