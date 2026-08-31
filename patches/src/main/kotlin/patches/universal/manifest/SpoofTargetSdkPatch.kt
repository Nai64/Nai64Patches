package patches.universal.manifest

import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

/**
 * Sets the app's targetSdkVersion in the manifest.
 *
 * Newer Android versions (and some installers / app stores) refuse to install
 * apps that target a too-low SDK level. Spoofing a higher targetSdkVersion
 * lets such apps install.
 *
 * The existing <uses-sdk> element is modified in place when present. A new
 * <uses-sdk> is only created when the manifest has none, and is inserted
 * before <application> so the manifest element ordering stays valid
 * (otherwise installers report the APK as corrupted/invalid).
 */
@Suppress("unused")
val spoofTargetSdkPatch = resourcePatch(
    name = "Spoof Target SDK",
    description = "Fixes install errors on newer Android versions.",
    default = false,
) {
    val targetSdk by intOption(
        title = "Target SDK version",
        default = 34,
        key = "targetSdkVersion",
        description = "Target SDK level to report in the manifest. Android blocks installing apps targeting below a minimum level on newer versions.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = targetSdk ?: 34
        if (target <= 0) {
            logger.warning("No valid target SDK configured. No changes applied.")
            return@execute
        }

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            if (root == null || root.tagName != "manifest") {
                logger.warning("No <manifest> root element found. No changes applied.")
                return@execute
            }

            val usesSdk = root.getElementsByTagName("uses-sdk")?.item(0) as? Element

            if (usesSdk != null) {
                usesSdk.setAttributeNS(NS_ANDROID, "android:targetSdkVersion", target.toString())
                logger.info("Updated <uses-sdk> targetSdkVersion to $target")
            } else {
                val created = manifest.createElement("uses-sdk")
                created.setAttributeNS(NS_ANDROID, "android:targetSdkVersion", target.toString())
                val application = root.applicationOrNull()
                if (application != null) {
                    root.insertBefore(created, application)
                } else {
                    root.appendChild(created)
                }
                logger.info("Created <uses-sdk> with targetSdkVersion $target")
            }
        }
    }
}
