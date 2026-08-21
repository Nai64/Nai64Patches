package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val bypassVpnDetectionPatch = bytecodePatch(
    name = "Bypass VPN Detection",
    description =
        "Makes NetworkCapabilities report no VPN: hasTransport always returns false and " +
            "hasCapability always returns true, so apps cannot block or alter behavior on VPN",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        fun forcedValue(reference: MethodReference): String? = when {
            reference.definingClass == "Landroid/net/NetworkCapabilities;" &&
                reference.name == "hasTransport" &&
                reference.returnType == "Z" -> "0x0"

            reference.definingClass == "Landroid/net/NetworkCapabilities;" &&
                reference.name == "hasCapability" &&
                reference.returnType == "Z" -> "0x1"

            else -> null
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
                    val value = forcedValue(reference) ?: continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        method.replaceInstruction(index, "const/4 v${next.registerA}, $value")
                        method.replaceInstruction(index + 1, "nop")
                    } else {
                        method.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Forced $patched NetworkCapabilities check(s)")
        } else {
            logger.warning("No NetworkCapabilities checks found. No changes applied.")
        }
    }
}
