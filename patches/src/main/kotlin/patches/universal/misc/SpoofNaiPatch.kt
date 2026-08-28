package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofNaiPatch = bytecodePatch(
    name = "Spoof NAI",
    description = "Reports a chosen string from TelephonyManager.getNai() so apps cannot read the Network Access Identifier.",
    default = false,
) {
    val nai by stringOption(
        title = "NAI",
        default = "",
        key = "nai",
        description = "NAI to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNai"),
            nai ?: "",
        )
        if (patched > 0) logger.info("Spoofed NAI at $patched call site(s)")
        else logger.warning("No getNai call sites found. No changes applied.")
    }
}
