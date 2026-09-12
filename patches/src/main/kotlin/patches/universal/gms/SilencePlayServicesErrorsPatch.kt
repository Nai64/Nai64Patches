package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object GmsErrorDialogFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "getErrorDialog",
    returnType = "Landroid/app/Dialog;",
)

internal object GmsErrorNotificationFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "showErrorNotification",
    returnType = "V",
)

internal object GmsErrorDialogFragmentFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "showErrorDialogFragment",
    returnType = "V",
)

internal object GmsUserResolvableErrorFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "isUserResolvableError",
    returnType = "Z",
)

internal object GmsErrorStringFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "getErrorString",
    returnType = "Ljava/lang/String;",
)

@Suppress("unused")
val silencePlayServicesErrorsPatch = bytecodePatch(
    name = "Silence Play Services Errors",
    description = "Kills Google Play Services error dialogs, notifications and resolvable-error flags. Apps stop nagging about missing or outdated Play Services. Pair with Bypass Google Play Services Check.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsNullAll(logger, GmsErrorDialogFingerprint, "Silence Play Services Errors: getErrorDialog")
        total += gmsNoopAll(logger, GmsErrorNotificationFingerprint, "Silence Play Services Errors: showErrorNotification")
        total += gmsNoopAll(logger, GmsErrorDialogFragmentFingerprint, "Silence Play Services Errors: showErrorDialogFragment")
        total += gmsZeroAll(logger, GmsUserResolvableErrorFingerprint, "Silence Play Services Errors: isUserResolvableError")
        total += gmsEmptyStringAll(logger, GmsErrorStringFingerprint, "Silence Play Services Errors: getErrorString")
        if (total == 0) logger.warning("Silence Play Services Errors: no error UI methods found. No changes applied.")
        else logger.info("Silence Play Services Errors: silenced $total method(s) in total")
    }
}
