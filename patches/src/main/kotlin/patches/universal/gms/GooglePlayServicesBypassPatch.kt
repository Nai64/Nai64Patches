package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import java.util.logging.Logger

internal object GoogleApiAvailabilityContextFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "isGooglePlayServicesAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/content/Context;"),
)

internal object GoogleApiAvailabilityContextVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "isGooglePlayServicesAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
)

internal object GoogleApiAvailabilityActivityFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "isGooglePlayServicesAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/app/Activity;"),
)

internal object GoogleApiAvailabilityActivityVersionFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GoogleApiAvailability;",
    name = "isGooglePlayServicesAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/app/Activity;", "I"),
)

internal object GooglePlayServicesUtilContextFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtil;",
    name = "isGooglePlayServicesAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf("Landroid/content/Context;"),
)

@Suppress("unused")
val bypassGooglePlayServicesCheckPatch = bytecodePatch(
    name = "Bypass Google Play Services Check",
    description = "Tricks the app into thinking Google Play Services is installed and working, so it does not disable features that depend on it.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val fingerprints = listOf(
            GoogleApiAvailabilityContextFingerprint,
            GoogleApiAvailabilityContextVersionFingerprint,
            GoogleApiAvailabilityActivityFingerprint,
            GoogleApiAvailabilityActivityVersionFingerprint,
            GooglePlayServicesUtilContextFingerprint,
        )

        var patched = 0
        for (fingerprint in fingerprints) {
            val method = fingerprint.methodOrNull ?: continue
            method.addInstructions(
                0,
                """
                const/4 v0, 0x0
                return v0
                """.trimIndent(),
            )
            patched++
        }

        if (patched == 0) {
            logger.warning("Google Play Services availability check not found. No changes applied.")
        } else {
            logger.info("Bypassed Google Play Services check ($patched method(s))")
        }
    }
}