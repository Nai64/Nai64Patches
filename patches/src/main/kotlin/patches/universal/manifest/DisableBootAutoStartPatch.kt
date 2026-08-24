package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

@Suppress("unused")
val disableBootAutoStartPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Removes <receiver> components that listen for android.intent.action.BOOT_COMPLETED so the app can no longer start itself on device boot.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            val receivers = manifest.getElementsByTagName("receiver")
            for (i in receivers.length - 1 downTo 0) {
                val receiver = receivers.item(i) as? Element ?: continue
                val filters = receiver.getElementsByTagName("intent-filter")
                var hasBoot = false
                for (f in 0 until filters.length) {
                    val filter = filters.item(f) as? Element ?: continue
                    val actions = filter.getElementsByTagName("action")
                    for (a in 0 until actions.length) {
                        val action = actions.item(a) as? Element ?: continue
                        val name = action.getAttributeNS(NS_ANDROID, "name")
                            .ifEmpty { action.getAttribute("android:name") }
                        if (name == BOOT_COMPLETED) {
                            hasBoot = true
                            break
                        }
                    }
                    if (hasBoot) break
                }
                if (hasBoot) {
                    receiver.parentNode?.removeChild(receiver)
                    removed++
                }
            }
        }
        if (removed > 0) logger.info("Removed $removed boot auto-start receiver(s)")
        else logger.warning("No boot auto-start receivers found. No changes applied.")
    }
}
