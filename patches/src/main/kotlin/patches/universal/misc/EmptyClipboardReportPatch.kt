package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val emptyClipboardReportPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes ClipboardManager.hasPrimaryClip always return false so apps stop harvesting or " +
            "acting on clipboard content copied from other apps",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/content/ClipboardManager;" to mapOf("hasPrimaryClip" to "0x0"),
            ),
        )

        if (patched > 0) logger.info("Forced $patched clipboard check(s) to empty")
        else logger.warning("No clipboard checks found. No changes applied.")
    }
}
