package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val relaxHardwareFeaturesPatch = resourcePatch(
    name = "Relax Hardware Features",
    description = "Sets android:required=\"false\" on every <uses-feature> so the app installs on devices that lack those hardware features.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Manifest") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var n = 0
        document("AndroidManifest.xml").use { manifest ->
            val features = manifest.getElementsByTagName("uses-feature")
            for (i in features.length - 1 downTo 0) {
                val f = features.item(i) as? Element ?: continue
                f.setAttributeNS(NS_ANDROID, "android:required", "false")
                n++
            }
        }
        if (n > 0) logger.info("Relaxed $n <uses-feature> requirement(s)")
        else logger.warning("No <uses-feature> elements found. No changes applied.")
    }
}
