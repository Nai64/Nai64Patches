package patches.universal.licensing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import java.util.logging.Logger

internal object LicenseCheckVerifyFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/vending/licensing/LicenseChecker;",
    name = "verify",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Lcom/google/android/vending/licensing/LicenseCheckerCallback;", "I"),
)

@Suppress("unused")
val bypassLicenseVerificationPatch = bytecodePatch(
    name = "Bypass License Verification",
    description = "Forces Google Play's license (LVL) check to pass, so the app does not refuse to run on unlicensed or modified builds.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val method = LicenseCheckVerifyFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            logger.warning("Google Play license checker not found. No changes applied.")
            return@execute
        }

        if (method.implementation!!.registerCount < 2) {
            logger.warning("Skipping license check bypass: not enough registers")
            return@execute
        }

        // Fire allow() on the callback (p1) with Policy.LICENSED (0x100)
        // and return, skipping the actual license verification.
        method.addInstructions(
            0,
            """
            const/16 v0, 0x100
            invoke-interface {p1, v0}, Lcom/google/android/vending/licensing/LicenseCheckerCallback;->allow(I)V
            return-void
            """.trimIndent(),
        )
        logger.info("License check bypassed")
    }
}