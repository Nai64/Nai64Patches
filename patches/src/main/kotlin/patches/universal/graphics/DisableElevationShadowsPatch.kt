package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.noOpVoidInvoke
import java.util.logging.Logger

@Suppress("unused")
val disableElevationShadowsPatch = bytecodePatch(
    name = "Disable Elevation Shadows",
    description = "Drops view elevation and shadow calls for a flatter, faster UI.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke(
            targetClass = "Landroid/view/View;",
            methods = setOf("setElevation", "setTranslationZ", "setOutlineProvider"),
        )
        if (patched > 0) logger.info("Disabled $patched elevation/shadow call(s)")
        else logger.warning("No elevation calls found. No changes applied.")
    }
}
