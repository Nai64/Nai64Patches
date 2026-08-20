package patches.universal.consent

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
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

internal object AppLovinShowConsentFlowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/sdk/AppLovinSdk;",
    name = "showConsentFlow",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Lcom/applovin/sdk/AppLovinSdk\$ConsentFlowListener;"),
)

@Suppress("unused")
val skipConsentPopupPatch = bytecodePatch(
    name = "Skip Consent Popup",
    description = "Skip the Google consent (GDPR) popup",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patched = false

        // AppLovin MAX consent flow (CCPA/GDPR dialog shown by the ad SDK).
        val appLovin = AppLovinShowConsentFlowFingerprint.methodOrNull
        if (appLovin != null) {
            appLovin.addInstruction(0, "return-void")
            logger.info("AppLovin consent flow skipped")
            patched = true
        }

        // Google UMP consent form (GDPR).
        val method = UmpConsentFormShowFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            if (!patched) {
                logger.warning("No consent popup found. No changes applied.")
            }
            return@execute
        }

        if (method.implementation!!.registerCount < 3) {
            logger.warning("Skipping Google consent popup: not enough registers")
            return@execute
        }

        // Fire onConsentFormDismissed on the listener (p2) with a null
        // FormError and return, skipping the consent dialog entirely.
        method.addInstructions(
            0,
            """
            const/4 v0, 0x0
            invoke-interface {p2, v0}, Lcom/google/android/ump/ConsentFormOnShowListener;->onConsentFormDismissed(Lcom/google/android/ump/FormError;)V
            return-void
            """.trimIndent(),
        )
        logger.info("Google consent popup skipped")
    }
}