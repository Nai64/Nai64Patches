package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val fakeOnlineStatePatch = bytecodePatch(
    name = "Fake Online State",
    description =
        "Makes connectivity checks report an active connection: WifiManager.isWifiEnabled, " +
            "NetworkInfo.isConnected/isAvailable/isConnectedOrConnecting all return true",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        fun isForcedTrue(reference: MethodReference): Boolean {
            if (reference.returnType != "Z") return false
            return when (reference.definingClass) {
                "Landroid/net/wifi/WifiManager;" -> reference.name == "isWifiEnabled"
                "Landroid/net/NetworkInfo;" ->
                    reference.name in setOf("isConnected", "isAvailable", "isConnectedOrConnecting")

                else -> false
            }
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                // Snapshot; one-for-one replacements keep indices valid.
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (!isForcedTrue(reference)) continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        // Fold invoke + move-result into a single constant.
                        method.replaceInstruction(index, "const/4 v${next.registerA}, 0x1")
                        method.replaceInstruction(index + 1, "nop")
                    } else {
                        // Result unused: dropping the call entirely is safe.
                        method.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Forced $patched connectivity check(s) to true")
        } else {
            logger.warning("No connectivity checks found. No changes applied.")
        }
    }
}
