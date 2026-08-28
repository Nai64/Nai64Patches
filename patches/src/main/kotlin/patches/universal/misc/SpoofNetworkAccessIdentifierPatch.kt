package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofNetworkAccessIdentifierPatch = bytecodePatch(
    name = "Spoof Network Access Identifier",
    description = "Reports a chosen string from TelephonyManager.getNai() so apps cannot read the network access identifier.",
    default = false,
) {
    val nai by stringOption(
        title = "Network Access Identifier",
        default = "",
        key = "nai",
        description = "Identifier to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNai"),
            nai ?: "",
        )
        if (patched > 0) logger.info("Spoofed network access identifier at $patched call site(s)")
        else logger.warning("No getNai call sites found. No changes applied.")
    }
}
