package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanGetterConst

@Suppress("unused")
val disableContentCapturePatch = bytecodePatch(
    name = "Disable Content Capture",
    description = "Reports content capture as disabled so apps stop sharing their view hierarchy with capture services",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanGetterConst(
            "Landroid/view/contentcapture/ContentCaptureManager;",
            setOf("isContentCaptureEnabled"),
            false,
        )
        if (patched > 0) logger.info("Disabled content capture at $patched call site(s)")
        else logger.warning("No content capture calls found. No changes applied.")
    }
}
