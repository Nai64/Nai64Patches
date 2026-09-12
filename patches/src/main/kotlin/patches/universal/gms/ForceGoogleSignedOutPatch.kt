package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object GoogleSignInHasPermissionsFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/auth/api/signin/GoogleSignIn;",
    name = "hasPermissions",
    returnType = "Z",
)

internal object GoogleSignInLastAccountFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/auth/api/signin/GoogleSignIn;",
    name = "getLastSignedInAccount",
    returnType = "Lcom/google/android/gms/auth/api/signin/GoogleSignInAccount;",
)

@Suppress("unused")
val forceGoogleSignedOutPatch = bytecodePatch(
    name = "Force Google Signed-Out",
    description = "Forces the Google signed-out state: permission checks fail and last-account returns null (documented nullable). Apps fall back to guest or offline mode instead of nagging for sign-in.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsZeroAll(logger, GoogleSignInHasPermissionsFingerprint, "Force Google Signed-Out: hasPermissions")
        total += gmsNullAll(logger, GoogleSignInLastAccountFingerprint, "Force Google Signed-Out: getLastSignedInAccount")
        if (total == 0) logger.warning("Force Google Signed-Out: no GoogleSignIn methods found. No changes applied.")
        else logger.info("Force Google Signed-Out: forced $total method(s) in total")
    }
}
