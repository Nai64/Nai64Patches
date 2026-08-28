package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val enableUnrestrictedClipboardAccessPatch = bytecodePatch(
    name = "Enable Unrestricted Clipboard Access",
    description = "Makes clipboard appear to have content so apps that require clipboard access keep working.",
    default = false,
) {
    val enabled by booleanOption(
        title = "Has content",
        default = true,
        key = "clipboardHasContent",
        description = "Report clipboard as having content.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = foldBooleanGetterConst(
            "Landroid/content/ClipboardManager;",
            setOf("hasPrimaryClip"),
            enabled == true,
        )
        patched += foldBooleanGetterConst(
            "Landroid/content/ClipboardManager;",
            setOf("hasText"),
            enabled == true,
        )
        if (patched > 0) logger.info("Enabled clipboard access at $patched call site(s)")
        else logger.warning("No clipboard hasPrimaryClip checks found. No changes applied.")
    }
}
