package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val allowMixedContentPatch = bytecodePatch(
    name = "Allow Mixed Content",
    description = "Forces WebSettings.setMixedContentMode(MIXED_CONTENT_ALWAYS_ALLOW) so WebViews load HTTP resources on HTTPS pages.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    if (insn !is ReferenceInstruction) continue
                    val ref = insn.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/webkit/WebSettings;") continue
                    if (ref.name != "setMixedContentMode" || ref.returnType != "V") continue
                    if (ref.parameterTypes != listOf("I")) continue

                    val reg = when (insn) {
                        is FiveRegisterInstruction -> insn.registerC
                        is RegisterRangeInstruction -> insn.startRegister + insn.registerCount - 1
                        else -> continue
                    }

                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode == Opcode.NOP) continue
                        if (prev is NarrowLiteralInstruction &&
                            prev is OneRegisterInstruction &&
                            prev.registerA == reg
                        ) {
                            method.replaceInstruction(j, "const/4 v$reg, 0x1")
                            patched++
                            break
                        }
                        break
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Forced $patched setMixedContentMode() call(s) to ALWAYS_ALLOW")
        } else {
            logger.warning("No setMixedContentMode() calls found. No changes applied.")
        }
    }
}
