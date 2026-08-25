package patches.bandcamp

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

private fun BytecodePatchContext.foldShowSearchResultScore(): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass != "Landroid/content/SharedPreferences;") continue
                if (ref.name != "getBoolean") continue
                if (ref.returnType != "Z") continue
                if (ref.parameterTypes.size != 2) continue
                if (ref.parameterTypes[0] != "Ljava/lang/String;") continue
                if (ref.parameterTypes[1] != "Z") continue

                val keyReg = when (insn) {
                    is BuilderInstruction35c -> insn.registerC
                    is BuilderInstruction3rc -> insn.startRegister + 1
                    else -> continue
                }

                var keyVal: String? = null
                for (j in index - 1 downTo 0) {
                    val prev = instructions[j]
                    if (prev.opcode != Opcode.CONST_STRING && prev.opcode != Opcode.CONST_STRING_JUMBO) continue
                    val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                    if (reg != keyReg) continue
                    keyVal = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                    break
                }
                if (keyVal != "show_search_result_score") continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    val resReg = (next as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "const/4 v$resReg, 0x1")
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                } else if (next == null || next.opcode != Opcode.MOVE_RESULT) {
                    // no move-result (unused) – just nop the invoke
                    method.replaceInstruction(index, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

@Suppress("unused")
val showSearchResultScoresPatch = bytecodePatch(
    name = "Show Search Result Scores",
    description = "Bandcamp: always shows relevance scores in search results (admin debug flag show_search_result_score).",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldShowSearchResultScore()
        if (patched > 0) logger.info("Forced $patched search-score check(s) to true")
        else logger.warning("No search-score checks found. No changes applied.")
    }
}
