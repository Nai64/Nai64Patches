package patches.universal.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object AdvertisingIdClientInfoGetIdFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;",
    name = "getId",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object AdvertisingIdClientInfoIsLimitAdTrackingFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;",
    name = "isLimitAdTrackingEnabled",
    returnType = "Z",
    parameters = emptyList(),
)

@Suppress("unused")
val limitAdTrackingPatch = bytecodePatch(
    name = "Limit Ad Tracking",
    description = "Neutralize the Google advertising ID: empty ID and limit ad tracking enabled",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patched = 0

        val getId = AdvertisingIdClientInfoGetIdFingerprint.methodOrNull
        if (getId != null && getId.implementation != null && getId.implementation!!.registerCount >= 1) {
            getId.addInstructions(
                0,
                """
                const-string v0, ""
                return-object v0
                """.trimIndent(),
            )
            patched++
        }

        val limit = AdvertisingIdClientInfoIsLimitAdTrackingFingerprint.methodOrNull
        if (limit != null && limit.implementation != null && limit.implementation!!.registerCount >= 1) {
            limit.addInstructions(
                0,
                """
                const/4 v0, 0x1
                return v0
                """.trimIndent(),
            )
            patched++
        }

        if (patched == 0) {
            logger.warning("Google AdvertisingIdClient not found. No changes applied.")
        } else {
            logger.info("Advertising ID neutralized ($patched method(s))")
        }
    }
}