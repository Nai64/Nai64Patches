package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceSinglePhonePatch = bytecodePatch(
    name = "Force Single Phone",
    description = "Makes TelephonyManager.getPhoneCount() report 1 so apps that branch on multiple phones/radios behave as a single-phone device.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getPhoneCount"),
            1,
        )
        if (patched > 0) logger.info("Forced $patched phone-count read(s) to 1")
        else logger.warning("No getPhoneCount call sites found. No changes applied.")
    }
}
