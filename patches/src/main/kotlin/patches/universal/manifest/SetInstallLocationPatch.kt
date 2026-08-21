package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val setInstallLocationPatch = resourcePatch(
    name = "Set Install Location",
    description = "Overrides the android:installLocation declared in the manifest",
    default = false,
) {
    val installLocation by stringOption(
        title = "Install location",
        default = "auto",
        values = linkedMapOf(
            "Auto (system decides)" to "auto",
            "Prefer external storage" to "preferExternal",
            "Internal storage only" to "internalOnly",
        ),
        key = "installLocation",
        description = "Where Android prefers to install the app.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = installLocation.orEmpty().trim()
        if (value !in setOf("auto", "preferExternal", "internalOnly")) {
            logger.warning("Invalid install location \"$value\". No changes applied.")
            return@execute
        }

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            if (root != null && root.tagName == "manifest") {
                root.setAttributeNS(NS_ANDROID, "android:installLocation", value)
                applied = true
            }
        }

        if (applied) {
            logger.info("Set install location to \"$value\"")
        } else {
            logger.warning("No <manifest> root element found. No changes applied.")
        }
    }
}
