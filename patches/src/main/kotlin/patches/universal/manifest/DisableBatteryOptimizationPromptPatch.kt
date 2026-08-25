package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private const val BATTERY_OPTIMIZATION_PERMISSION =
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"

private val batteryOptimizationActions = setOf(
    "android.settings.action.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.settings.action.IGNORE_BATTERY_OPTIMIZATION_SETTINGS",
)

@Suppress("unused")
val disableBatteryOptimizationPromptPatch = resourcePatch(
    name = "Disable Battery Optimization Prompt",
    description =
        "Removes the permission and intent filters apps use to ask for battery optimization " +
            "exemptions, so the 'Ignore battery optimizations?' dialog never appears",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var removedPermissions = 0
        var removedFilters = 0
        document("AndroidManifest.xml").use { manifest ->
            // Drop the exemption permission.
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in permissions.length - 1 downTo 0) {
                val element = permissions.item(i) as? Element ?: continue
                if (element.getAttributeNS(NS_ANDROID, "name") == BATTERY_OPTIMIZATION_PERMISSION ||
                    element.getAttribute("android:name") == BATTERY_OPTIMIZATION_PERMISSION
                ) {
                    element.parentNode?.removeChild(element)
                    removedPermissions++
                }
            }

            // Drop intent filters that request the exemption dialogs.
            val filters = manifest.getElementsByTagName("intent-filter")
            for (i in filters.length - 1 downTo 0) {
                val filter = filters.item(i) as? Element ?: continue
                val actions = filter.getElementsByTagName("action")
                for (j in 0 until actions.length) {
                    val action = actions.item(j) as? Element ?: continue
                    val name = action.getAttributeNS(NS_ANDROID, "name").ifEmpty {
                        action.getAttribute("android:name")
                    }
                    if (name in batteryOptimizationActions) {
                        filter.parentNode?.removeChild(filter)
                        removedFilters++
                        break
                    }
                }
            }
        }

        when {
            removedPermissions > 0 || removedFilters > 0 -> logger.info(
                "Removed $removedPermissions permission(s) and $removedFilters intent filter(s)",
            )

            else -> logger.warning("No battery optimization entries found. No changes applied.")
        }
    }
}
