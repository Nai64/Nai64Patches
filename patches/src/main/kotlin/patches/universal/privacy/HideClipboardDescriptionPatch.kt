package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideClipboardDescriptionPatch = bytecodePatch(
    name = "Hide Clipboard Description",
    description = "Makes ClipboardManager.getPrimaryClipDescription return null to block MIME and type harvesting",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            definingClass = "Landroid/content/ClipboardManager;",
            methodNames = setOf("getPrimaryClipDescription"),
            returnType = "Landroid/content/ClipDescription;",
        )
        if (patched > 0) logger.info("Hid clipboard description at $patched call site(s)")
        else logger.warning("No getPrimaryClipDescription calls found. No changes applied.")
    }
}
