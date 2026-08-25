package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val relaxRequiredLibrariesPatch = resourcePatch(
    name = "Relax Required Libraries",
    description = "Sets android:required=\"false\" on every <uses-library> so the app installs even when optional system libraries are missing.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var n = 0
        document("AndroidManifest.xml").use { manifest ->
            val libs = manifest.getElementsByTagName("uses-library")
            for (i in libs.length - 1 downTo 0) {
                val l = libs.item(i) as? Element ?: continue
                l.setAttributeNS(NS_ANDROID, "android:required", "false")
                n++
            }
        }
        if (n > 0) logger.info("Relaxed $n <uses-library> requirement(s)")
        else logger.warning("No <uses-library> elements found. No changes applied.")
    }
}
