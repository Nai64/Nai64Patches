package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val alwaysAllowBiometricsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes BiometricManager.canAuthenticate always return BIOMETRIC_SUCCESS so apps do " +
            "not block biometric features on unsupported or unenrolled devices",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // Both overloads (API 28 and API 30) return int status codes where 0 == success.
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/hardware/biometrics/BiometricManager;" to mapOf("canAuthenticate" to "0x0"),
            ),
            returnType = "I",
        )

        if (patched > 0) logger.info("Forced $patched biometric capability check(s) to success")
        else logger.warning("No biometric checks found. No changes applied.")
    }
}
