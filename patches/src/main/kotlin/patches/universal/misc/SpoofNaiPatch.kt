package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofNaiPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports an empty string from TelephonyManager.getNai() so apps cannot read the Network Access Identifier.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNai"),
            "",
        )
        if (patched > 0) logger.info("Spoofed NAI at $patched call site(s)")
        else logger.warning("No getNai call sites found. No changes applied.")
    }
}
