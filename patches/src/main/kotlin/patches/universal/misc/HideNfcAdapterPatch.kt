package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideNfcAdapterPatch = bytecodePatch(
    name = "Hide Nfc Adapter",
    description = "Makes NfcAdapter.getDefaultAdapter return null so NFC probes fail uniformly",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            definingClass = "Landroid/nfc/NfcAdapter;",
            methodNames = setOf("getDefaultAdapter"),
            returnType = "Landroid/nfc/NfcAdapter;",
        )
        if (patched > 0) logger.info("Hid NfcAdapter at $patched call site(s)")
        else logger.warning("No NfcAdapter.getDefaultAdapter calls found. No changes applied.")
    }
}
