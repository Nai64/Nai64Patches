package patches.gemini

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val CONTEXT_NAMES = setOf(
    "getMaxTokens", "getContextLimit", "getMaxContextLength", "getMaxContextTokens",
    "getTokenLimit", "getMaxHistoryTokens", "getContextWindowSize", "getMaxInputTokens",
)

private fun BytecodePatchContext.foldContextLimits(): Int {
    var patched = 0
    val value = 1000000
    val literal = value.toString()
    val constInstr = when {
        value in -8..7 -> "const/4 v{reg}, $literal"
        value in -32768..32767 -> "const/16 v{reg}, $literal"
        else -> "const v{reg}, $literal"
    }
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name !in CONTEXT_NAMES) continue
                if (ref.returnType != "I") continue
                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    val reg = next.registerA
                    val instr = constInstr.replace("{reg}", reg.toString())
                    method.replaceInstruction(index, instr)
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val increaseContextLimitPatch = bytecodePatch(
    name = "Increase Context Limit",
    description = "Gemini: increases hidden context/token limits (getMaxTokens/getContextLimit) to 1M tokens.",
    default = false,
) {
    compatibleWith("com.google.android.apps.bard")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldContextLimits()
        if (patched > 0) logger.info("Increased $patched context limit(s) to 1M")
        else logger.warning("No context limit checks found. No changes applied.")
    }
}
