package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeFingerprintHardwarePatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes FingerprintManager report available hardware and enrolled fingerprints so " +
            "apps unlock fingerprint-gated features on any device",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/hardware/fingerprint/FingerprintManager;" to mapOf(
                    "isHardwareDetected" to "0x1",
                    "hasEnrolledFingerprints" to "0x1",
                ),
            ),
        )

        if (patched > 0) logger.info("Forced $patched fingerprint check(s)")
        else logger.warning("No fingerprint checks found. No changes applied.")
    }
}
