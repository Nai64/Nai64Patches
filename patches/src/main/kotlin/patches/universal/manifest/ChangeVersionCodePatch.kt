package patches.universal.manifest

import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val changeVersionCodePatch = resourcePatch(
    name = "Change Version Code",
    description = "Overrides the android:versionCode declared in the manifest",
    default = false,
) {
    val versionCode by intOption(
        title = "Version code",
        default = -1,
        key = "versionCode",
        description = "New version code. Use -1 (default) to keep the original.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val code = versionCode ?: -1
        if (code <= 0) {
            logger.warning("No valid version code configured. No changes applied.")
            return@execute
        }

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            if (root != null && root.tagName == "manifest") {
                root.setAttributeNS(NS_ANDROID, "android:versionCode", code.toString())
                applied = true
            }
        }

        if (applied) {
            logger.info("Changed version code to $code")
        } else {
            logger.warning("No <manifest> root element found. No changes applied.")
        }
    }
}
