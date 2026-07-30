package patches.universal.resolution

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val unityPlayerActivityOnCreateFingerprint = Fingerprint(
    definingClass = "Lcom/unity3d/player/UnityPlayerActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

private val manifestResolutionPatch = resourcePatch(
    name = "Custom App Resolution Manifest (internal)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as? Element ?: run {
                logger.warning("No <application> element found. Skipping manifest changes.")
                return@execute
            }
            val ns = "http://schemas.android.com/apk/res/android"

            // Enable resizeableActivity
            app.setAttributeNS(ns, "android:resizeableActivity", "true")

            // Find the launcher activity and set maxAspectRatio
            val activities = doc.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                val isLauncher = activity.getElementsByTagName("intent-filter").let { filters ->
                    var found = false
                    for (j in 0 until filters.length) {
                        val filter = filters.item(j) as? Element ?: continue
                        val actions = filter.getElementsByTagName("action")
                        for (k in 0 until actions.length) {
                            val action = actions.item(k) as? Element ?: continue
                            if (action.getAttributeNS(ns, "name") == "android.intent.action.MAIN") {
                                found = true
                                break
                            }
                        }
                        if (found) break
                    }
                    found
                }
                if (isLauncher) {
                    activity.setAttributeNS(ns, "android:resizeableActivity", "true")
                    // The maxAspectRatio will be set from the bytecode patch options
                    logger.info("Set resizeableActivity on launcher activity")
                }
            }
            logger.info("Custom App Resolution Manifest (internal) patch succeeded")
        }
    }
}

@Suppress("unused")
val customResolutionPatch = bytecodePatch(
    name = "Custom App Resolution (Experimental)",
    default = false,
) {
    dependsOn(manifestResolutionPatch)

    val enableCustomResolution by booleanOption(
        title = "Enable Custom Resolution",
        default = false,
        key = "enableCustomResolution",
        description = "",
    )
    val width by intOption(
        title = "Resolution width (px)",
        default = 1920,
        key = "width",
        description = "",
    )
    val height by intOption(
        title = "Resolution height (px)",
        default = 1080,
        key = "height",
        description = "",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        if (enableCustomResolution != true) {
            logger.info("Custom resolution not enabled. Skipping.")
            return@execute
        }

        val w = width ?: 0
        val h = height ?: 0
        if (w <= 0 || h <= 0) {
            logger.warning("Invalid resolution: ${w}x${h}. Skipping.")
            return@execute
        }

        val match = unityPlayerActivityOnCreateFingerprint.methodOrNull
        if (match == null) {
            logger.warning("No Unity activity found. Manifest changes applied only.")
            return@execute
        }

        // Inject getWindow().setLayout(width, height) + FLAG_LAYOUT_NO_LIMITS
        // Assumes standard register layout: p0=this, p1=Bundle
        // Use v0/v1 for loaded values, v2 for window reference
        match.addInstructions(0, """
            invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
            move-result-object v2
            const v0, ${w}
            const v1, ${h}
            invoke-virtual {v2, v0, v1}, Landroid/view/Window;->setLayout(II)V
            const/16 v0, 0x200
            invoke-virtual {v2, v0}, Landroid/view/Window;->addFlags(I)V
        """.trimIndent())

        logger.info("Custom App Resolution (Experimental) patch succeeded — ${w}x${h}")
    }
}
