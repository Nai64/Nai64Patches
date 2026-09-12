package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldBooleanGetterConst
import java.util.logging.Logger

@Suppress("unused")
val forceHardwareLayersPatch = bytecodePatch(
    name = "Force Hardware Layers",
    description = "Makes hardware-acceleration checks report true for smoother rendering.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldBooleanGetterConst(
            definingClass = "Landroid/view/View;",
            methodNames = setOf("isHardwareAccelerated"),
            value = true,
        )
        patched += foldBooleanGetterConst(
            definingClass = "Landroid/graphics/Canvas;",
            methodNames = setOf("isHardwareAccelerated"),
            value = true,
        )
        if (patched > 0) logger.info("Forced $patched hardware-acceleration check(s) to true")
        else logger.warning("No hardware-acceleration checks found. No changes applied.")
    }
}
