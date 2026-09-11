package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceAndroidBeamPatch = bytecodePatch(
    name = "Force Android Beam",
    description = "Makes NfcAdapter.isNdefPushEnabled() report true so apps that require Android Beam (NDEF push) run normally.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Force") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf("Landroid/nfc/NfcAdapter;" to mapOf("isNdefPushEnabled" to "0x1")),
        )
        if (patched > 0) logger.info("Forced $patched android-beam check(s) to true")
        else logger.warning("No android-beam checks found. No changes applied.")
    }
}
