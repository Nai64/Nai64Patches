package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofPhoneNumberPatch = bytecodePatch(
    name = "Spoof Phone Number",
    description = "Reports a chosen phone number from TelephonyManager.getLine1Number() and getLine2Number() so apps cannot read the device's phone number.",
    default = false,
) {
    val number by stringOption(
        title = "Phone number",
        default = "",
        key = "phoneNumber",
        description = "Phone number to report (empty hides it).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getLine1Number", "getLine2Number"),
            value = number ?: "",
        )

        if (patched > 0) {
            logger.info("Spoofed $patched phone-number read(s)")
        } else {
            logger.warning("No phone-number reads found. No changes applied.")
        }
    }
}
