package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object GmsApkVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "getApkVersion",
    returnType = "I",
)

internal object GmsClientVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "getClientVersion",
    returnType = "I",
)

internal object GmsLightApkVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "getApkVersion",
    returnType = "I",
)

internal object GmsLightClientVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "getClientVersion",
    returnType = "I",
)

@Suppress("unused")
val spoofPlayServicesVersionPatch = bytecodePatch(
    name = "Spoof Play Services Version",
    description = "Reports a high Play Services version (25000000) so apps stop demanding a Play Services update. Useful on MicroG and de-Googled ROMs.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsVersionAll(logger, GmsApkVersionFingerprint, "Spoof Play Services Version: getApkVersion")
        total += gmsVersionAll(logger, GmsClientVersionFingerprint, "Spoof Play Services Version: getClientVersion")
        total += gmsVersionAll(logger, GmsLightApkVersionFingerprint, "Spoof Play Services Version: light getApkVersion")
        total += gmsVersionAll(logger, GmsLightClientVersionFingerprint, "Spoof Play Services Version: light getClientVersion")
        if (total == 0) logger.warning("Spoof Play Services Version: no version methods found. No changes applied.")
        else logger.info("Spoof Play Services Version: spoofed $total method(s) in total")
    }
}
