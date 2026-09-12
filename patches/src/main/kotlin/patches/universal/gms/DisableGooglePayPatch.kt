package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object GPayIsReadyToPayFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/wallet/PaymentsClient;",
    name = "isReadyToPay",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

@Suppress("unused")
val disableGooglePayPatch = bytecodePatch(
    name = "Disable Google Pay",
    description = "Reports Google Pay as unavailable so GPay buttons and prompts never appear. Does not touch the app's own checkout flow.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val total = gmsTaskBoolAll(logger, GPayIsReadyToPayFingerprint, "Disable Google Pay: isReadyToPay", false)
        if (total == 0) logger.warning("Disable Google Pay: no PaymentsClient found. No changes applied.")
        else logger.info("Disable Google Pay: disabled $total method(s)")
    }
}
