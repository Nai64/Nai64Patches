package patches.vivo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption

/*
 * Target: com.vivo.vivotws
 *
 * The "third app" device list only shows paired earphones whose name appears
 * in the app's Vivo TWS config (ThirdAppTwsConfigManager.g). Devices that are
 * not in that config are dropped from the list, ignored by the click handler,
 * and their model lookup (d) returns -1. Both methods are overridden below so
 * any paired Bluetooth audio device is shown and treated as a Vivo TWS
 * earphone.
 *
 * Vivo moved this class from util/i to util/j in 5.x; both are handled.
 */

internal object IsSupportedDeviceFingerprint : Fingerprint(
    definingClass = "Lcom/vivo/ui/base/thirdapp/util/i;",
    name = "g",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
)

internal object GetModelByNameFingerprint : Fingerprint(
    definingClass = "Lcom/vivo/ui/base/thirdapp/util/i;",
    name = "d",
    returnType = "I",
    parameters = listOf("Ljava/lang/String;"),
)

internal object IsSupportedDeviceFingerprintV5 : Fingerprint(
    definingClass = "Lcom/vivo/ui/base/thirdapp/util/j;",
    name = "g",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
)

internal object GetModelByNameFingerprintV5 : Fingerprint(
    definingClass = "Lcom/vivo/ui/base/thirdapp/util/j;",
    name = "d",
    returnType = "I",
    parameters = listOf("Ljava/lang/String;"),
)

@Suppress("unused")
val acceptAnyEarphonesPatch = bytecodePatch(
    name = "Accept any earphones",
    description = "Show any paired Bluetooth audio device as a Vivo TWS earphone so it can be selected and connected.",
    default = false,
) {
    compatibleWith(Compatibility(packageName = "com.vivo.vivotws", name = "vEarphones"))
    category("Misc")

    val spoofedModel by intOption(
        key = "vivoSpoofedModel",
        default = 16,
        title = "Spoofed TWS model",
        description = "Model reported to the app. 16 = vivo TWS Neo, 1 = vivo TWS 1, 28 = vivo TWS 2.",
    )

    execute {
        val context = this@execute
        val model = (spoofedModel ?: 16).coerceIn(1, 32767)
        var applied = 0

        for ((supported, modelLookup) in listOf(
            IsSupportedDeviceFingerprintV5 to GetModelByNameFingerprintV5,
            IsSupportedDeviceFingerprint to GetModelByNameFingerprint,
        )) {
            val supportedMethod = try {
                with(context) { supported.matchOrNull()?.method }
            } catch (_: Exception) {
                null
            } ?: continue
            val modelMethod = try {
                with(context) { modelLookup.matchOrNull()?.method }
            } catch (_: Exception) {
                null
            } ?: continue

            supportedMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            modelMethod.addInstructions(
                0,
                """
                    const/16 v0, $model
                    return v0
                """.trimIndent(),
            )
            applied++
        }

        check(applied > 0) { "Accept any earphones: no supported Vivo TWS config manager found" }
    }
}
