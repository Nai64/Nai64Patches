package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofIsimImpiPatch = bytecodePatch(
    name = "Spoof ISIM IMPI",
    description = "Reports a chosen string from TelephonyManager.getIsimImpi() so apps cannot read the ISIM private identity.",
    default = false,
) {
    val impi by stringOption(
        title = "IMPI",
        default = "",
        key = "isimImpi",
        description = "IMPI to report.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getIsimImpi"),
            impi ?: "",
        )
        if (patched > 0) logger.info("Spoofed ISIM IMPI at $patched call site(s)")
        else logger.warning("No getIsimImpi call sites found. No changes applied.")
    }
}
