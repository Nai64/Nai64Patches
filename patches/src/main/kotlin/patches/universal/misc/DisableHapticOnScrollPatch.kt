package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableHapticOnScrollPatch = bytecodePatch(
    name = "Disable Haptic On Scroll",
    description = "Prevents haptic feedback triggered by scrolling.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = foldBooleanReturns(
            mapOf(
                "Landroid/view/View;" to mapOf("performHapticFeedback" to "0x0"),
            ),
            returnType = "Z",
        )
        // Also cover HapticFeedbackConstants direct checks if any
        patched += foldBooleanGetterConst(
            "Landroid/view/HapticFeedbackConstants;",
            setOf("isHapticFeedbackEnabled"),
            false,
        )
        if (patched > 0) logger.info("Disabled haptic on scroll at $patched call site(s)")
        else logger.warning("No haptic feedback call sites found. No changes applied.")
    }
}
