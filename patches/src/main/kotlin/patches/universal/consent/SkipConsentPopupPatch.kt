package patches.universal.consent

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object UmpConsentFormShowFingerprint : Fingerprint(
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Lcom/google/android/ump/ConsentFormOnShowListener;"),
    custom = { method, classDef ->
        classDef.interfaces.contains("Lcom/google/android/ump/ConsentForm;")
    },
)

@Suppress("unused")
val skipConsentPopupPatch = bytecodePatch(
    name = "Skip Consent Popup",
    description = "Skip the Google consent (GDPR) popup",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val method = UmpConsentFormShowFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            logger.warning("Google consent form not found. No changes applied.")
            return@execute
        }

        if (method.implementation!!.registerCount < 3) {
            logger.warning("Skipping consent popup: not enough registers")
            return@execute
        }

        // Fire onConsentFormDismissed on the listener (p2) and return,
        // skipping the consent dialog entirely.
        method.addInstructions(
            0,
            """
            invoke-interface {p2}, Lcom/google/android/ump/ConsentFormOnShowListener;->onConsentFormDismissed()V
            return-void
            """.trimIndent(),
        )
        logger.info("Consent popup skipped")
    }
}