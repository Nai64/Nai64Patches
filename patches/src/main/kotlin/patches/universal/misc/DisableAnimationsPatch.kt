package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableAnimationsPatch = bytecodePatch(
    name = "Disable Animations",
    description =
        "Forces Settings.Global window/transition/animator scale values to 0 so the app " +
            "runs without UI animations (faster, less motion).",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldSettingsGetterConst(
            classes = setOf("Landroid/provider/Settings\$Global;"),
            keys = setOf(
                "window_animation_scale",
                "transition_animation_scale",
                "animator_duration_scale",
            ),
        )

        if (patched > 0) {
            logger.info("Disabled $patched animation-scale read(s)")
        } else {
            logger.warning("No animation-scale reads found. No changes applied.")
        }
    }
}
