package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideEmulatorRadioPatch = bytecodePatch(
    name = "Hide Emulator Radio",
    description =
        "Makes TelephonyManager.getPhoneType always report GSM so emulator detection based on " +
            "a missing radio reports a normal device",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // PHONE_TYPE_NONE (0) is what emulators report; PHONE_TYPE_GSM (1) is normal.
        val patched = foldBooleanReturns(
            mapOf("Landroid/telephony/TelephonyManager;" to mapOf("getPhoneType" to "0x1")),
            returnType = "I",
        )

        if (patched > 0) logger.info("Forced $patched radio-type check(s) to GSM")
        else logger.warning("No radio-type checks found. No changes applied.")
    }
}
