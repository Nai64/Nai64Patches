package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeNfcEnabledPatch = bytecodePatch(
    name = "Fake NFC Enabled",
    description =
        "Makes NfcAdapter.isEnabled always return true so apps that require NFC stop blocking " +
            "features on devices without it",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf("Landroid/nfc/NfcAdapter;" to mapOf("isEnabled" to "0x1")),
        )

        if (patched > 0) logger.info("Forced $patched NFC state check(s) to enabled")
        else logger.warning("No NFC checks found. No changes applied.")
    }
}
