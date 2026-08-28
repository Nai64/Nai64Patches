package patches.universal.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.Base64
import java.util.logging.Logger

@Suppress("unused")
val spoofPlayIntegrityPatch = bytecodePatch(
    name = "Spoof Play Integrity",
    description = "Forces Google Play Integrity attestation to pass, so the app runs on modified or unsupported devices instead of being blocked.",
    default = true,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // -- Build a fake passing verdict token (JWT) --
        // Uses the app's real package identity so client-side checks that
        // compare requestPackageName/packageName still pass.
        val packageName = packageMetadata.packageName
        val versionCode = packageMetadata.versionCode
        val timestamp = System.currentTimeMillis()

        val header = """{"alg":"RS256","typ":"JWT"}"""
        val payload = """{
            "requestDetails": {
                "requestPackageName": "$packageName",
                "timestampMillis": "$timestamp",
                "nonce": "morphe-spoof"
            },
            "appIntegrity": {
                "appRecognitionVerdict": "PLAY_RECOGNIZED",
                "packageName": "$packageName",
                "versionCode": "$versionCode",
                "certificateSha256Digest": ["AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="]
            },
            "deviceIntegrity": {
                "deviceRecognitionVerdict": ["MEETS_DEVICE_INTEGRITY"]
            },
            "accountDetails": {
                "appLicensingVerdict": "LICENSED"
            }
        }"""

        fun b64(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

        val token = b64(header) + "." + b64(payload) + "." + b64("spoofed-by-morphe")

        // -- Patch the token response methods --
        var patched = false

        val classic = IntegrityTokenResponseFingerprint.methodOrNull
        if (classic != null && classic.implementation != null) {
            if (classic.implementation!!.registerCount >= 1) {
                classic.addInstructions(
                    0,
                    """
                    const-string v0, "$token"
                    return-object v0
                    """.trimIndent(),
                )
                logger.info("Play Integrity spoofed (classic IntegrityTokenResponse)")
                patched = true
            } else {
                logger.warning("Skipping classic Play Integrity spoof: no free register")
            }
        }

        val standard = StandardIntegrityTokenResponseFingerprint.methodOrNull
        if (standard != null && standard.implementation != null) {
            if (standard.implementation!!.registerCount >= 1) {
                standard.addInstructions(
                    0,
                    """
                    const-string v0, "$token"
                    return-object v0
                    """.trimIndent(),
                )
                logger.info("Play Integrity spoofed (StandardIntegrityTokenResponse)")
                patched = true
            } else {
                logger.warning("Skipping Standard Play Integrity spoof: no free register")
            }
        }

        if (!patched) {
            logger.warning("Play Integrity SDK not found. No changes applied.")
        }
    }
}