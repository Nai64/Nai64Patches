package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
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

private fun BytecodePatchContext.forceIntSetter(
    targetClass: String,
    setters: Set<String>,
    value: Int,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name !in setters) continue
                if (ref.returnType != "V") continue
                if (ref.parameterTypes != listOf("I")) continue

                val reg = when (insn) {
                    is FiveRegisterInstruction -> insn.registerD
                    is RegisterRangeInstruction -> insn.startRegister + insn.registerCount - 1
                    else -> continue
                }

                val constSmali = if (value in -8..7) {
                    "const/4 v$reg, 0x${value.toString(16)}"
                } else {
                    "const/16 v$reg, 0x${value.toString(16)}"
                }

                for (j in index - 1 downTo 0) {
                    val prev = instructions[j]
                    if (prev.opcode == Opcode.NOP) continue
                    if (prev is NarrowLiteralInstruction &&
                        prev is OneRegisterInstruction &&
                        prev.registerA == reg
                    ) {
                        method.replaceInstruction(j, constSmali)
                        patched++
                        break
                    }
                    break
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val enableWebViewCachePatch = bytecodePatch(
    name = "Enable WebView Cache",
    description = "Forces WebSettings.setCacheMode(LOAD_CACHE_ELSE_NETWORK) so WebViews reuse cached resources and work better offline.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = forceIntSetter("Landroid/webkit/WebSettings;", setOf("setCacheMode"), 1)
        if (patched > 0) {
            logger.info("Forced $patched setCacheMode() call(s) to LOAD_CACHE_ELSE_NETWORK")
        } else {
            logger.warning("No setCacheMode() calls found. No changes applied.")
        }
    }
}
