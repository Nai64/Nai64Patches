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

private fun BytecodePatchContext.forceBooleanSetter(
    targetClass: String,
    setters: Set<String>,
    value: Boolean,
): Int {
    val literal = if (value) "0x1" else "0x0"
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
                if (ref.parameterTypes != listOf("Z")) continue

                val reg = when (insn) {
                    is FiveRegisterInstruction -> insn.registerD
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
                        method.replaceInstruction(j, "const/4 v$reg, $literal")
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
val enableWebViewZoomPatch = bytecodePatch(
    name = "Enable WebView Zoom",
    description = "Enables built-in pinch zoom in WebViews and hides the zoom controls for a cleaner view.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val a = forceBooleanSetter("Landroid/webkit/WebSettings;", setOf("setBuiltInZoomControls"), true)
        val b = forceBooleanSetter("Landroid/webkit/WebSettings;", setOf("setDisplayZoomControls"), false)
        val patched = a + b
        if (patched > 0) {
            logger.info("Forced $patched WebSettings zoom flag(s)")
        } else {
            logger.warning("No WebSettings zoom setters found. No changes applied.")
        }
    }
}
