package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofPhoneNumberPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Reports an empty string from TelephonyManager.getLine1Number() and " +
            "getLine2Number() so apps cannot read the device's phone number.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getLine1Number", "getLine2Number"),
            value = "",
        )

        if (patched > 0) {
            logger.info("Spoofed $patched phone-number read(s)")
        } else {
            logger.warning("No phone-number reads found. No changes applied.")
        }
    }
}
