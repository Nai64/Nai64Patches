package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofVoiceMailPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Reports empty strings from TelephonyManager.getVoiceMailNumber() and " +
            "getVoiceMailAlphaTag() so apps cannot read the voicemail number or tag.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getVoiceMailNumber", "getVoiceMailAlphaTag"),
            value = "",
        )

        if (patched > 0) {
            logger.info("Spoofed $patched voice-mail read(s)")
        } else {
            logger.warning("No voice-mail reads found. No changes applied.")
        }
    }
}
