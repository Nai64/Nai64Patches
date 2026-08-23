package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofIsimImpiPatch = bytecodePatch(
    name = "Spoof ISIM IMPI",
    description = "Reports an empty string from TelephonyManager.getIsimImpi() so apps cannot read the ISIM private identity.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/telephony/TelephonyManager;",
            setOf("getIsimImpi"),
            "",
        )
        if (patched > 0) logger.info("Spoofed ISIM IMPI at $patched call site(s)")
        else logger.warning("No getIsimImpi call sites found. No changes applied.")
    }
}
