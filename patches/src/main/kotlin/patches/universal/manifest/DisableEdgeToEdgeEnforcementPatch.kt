package patches.universal.manifest

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val disableEdgeToEdgeEnforcementPatch = resourcePatch(
    name = "Disable Edge-To-Edge Enforcement",
    description = "Opts out of Android 15 mandatory edge-to-edge so apps that break with enforced edge-to-edge render normally.",
    default = false,
) {
    val optOut by booleanOption(
        title = "Opt out",
        default = true,
        key = "optOutEdgeToEdge",
        description = "If true, sets windowOptOutEdgeToEdgeEnforcement to true.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        if (optOut != true) {
            logger.info("Opt-out disabled, skipping.")
            return@execute
        }
        var patched = 0
        document("AndroidManifest.xml").use { doc ->
            val app = doc.documentElement.applicationOrNull() ?: return@use
            // Application level
            app.setAttributeNS(NS_ANDROID, "android:windowOptOutEdgeToEdgeEnforcement", "true")
            patched++
            // Also apply to all activities for completeness
            val activities = doc.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val act = activities.item(i) as? Element ?: continue
                act.setAttributeNS(NS_ANDROID, "android:windowOptOutEdgeToEdgeEnforcement", "true")
                patched++
            }
        }
        logger.info("Disabled edge-to-edge enforcement at $patched node(s)")
    }
}
