package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val filteredComponentTags = listOf("activity", "activity-alias", "service", "receiver")

private fun Element.hasLauncherFilter(): Boolean {
    val actions = getElementsByTagName("action")
    var hasMainAction = false
    for (i in 0 until actions.length) {
        val action = actions.item(i) as? Element ?: continue
        if (action.getAttributeNS(NS_ANDROID, "name") == "android.intent.action.MAIN") {
            hasMainAction = true
            break
        }
    }
    if (!hasMainAction) return false

    val categories = getElementsByTagName("category")
    for (i in 0 until categories.length) {
        val category = categories.item(i) as? Element ?: continue
        if (category.getAttributeNS(NS_ANDROID, "name") == "android.intent.category.LAUNCHER") {
            return true
        }
    }
    return false
}

@Suppress("unused")
val repairMissingComponentExportFlagsPatch = resourcePatch(
    name = "Repair Missing Component Export Flags",
    description = "Adds missing android:exported to components with intent filters so the app installs on newer Android.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var repaired = 0

        document("AndroidManifest.xml").use { manifest ->
            for (tagName in filteredComponentTags) {
                val components = manifest.getElementsByTagName(tagName)
                for (i in 0 until components.length) {
                    val component = components.item(i) as? Element ?: continue
                    if (component.hasAttributeNS(NS_ANDROID, "exported")) continue
                    if (component.getElementsByTagName("intent-filter").length == 0) continue

                    val exported = component.hasLauncherFilter()
                    component.setAttributeNS(NS_ANDROID, "android:exported", exported.toString())
                    repaired++
                }
            }
        }

        if (repaired > 0) logger.info("Repaired $repaired component exported flag(s)")
        else logger.info("No filtered components with missing exported flags found. No changes applied.")
    }
}
