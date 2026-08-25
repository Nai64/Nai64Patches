package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val disableAppLinksVerificationPatch = resourcePatch(
    name = "Disable App Links Verification",
    description =
        "Removes android:autoVerify from intent filters so Android stops prompting to verify " +
            "the app's web domains and never converts links into app opens",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            val filters = manifest.getElementsByTagName("intent-filter")
            for (i in 0 until filters.length) {
                val filter = filters.item(i) as? Element ?: continue
                if (filter.getAttributeNS(NS_ANDROID, "autoVerify") == "true") {
                    filter.removeAttributeNS(NS_ANDROID, "autoVerify")
                    removed++
                }
            }
        }

        if (removed > 0) {
            logger.info("Removed autoVerify from $removed intent filter(s)")
        } else {
            logger.warning("No verified app links found. No changes applied.")
        }
    }
}
