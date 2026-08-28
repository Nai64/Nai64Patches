package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

@Suppress("unused")
val fakePointerLocationPatch = bytecodePatch(
    name = "Fake Pointer Location",
    description = "Reports pointer location as disabled via Settings so apps that check it stop doing so.",
    default = false,
) {
    val enabled by booleanOption(
        title = "Pointer location",
        default = false,
        key = "pointerLocation",
        description = "Report pointer location as enabled or disabled.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = if (enabled == true) 1 else 0
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions: List<Instruction> = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/provider/Settings\$System;") continue
                    if (ref.name != "getInt") continue
                    if (ref.returnType != "I") continue
                    val params = ref.parameterTypes
                    if (params.size < 2) continue
                    if (params[0] != "Landroid/content/ContentResolver;") continue
                    if (params[1] != "Ljava/lang/String;") continue
                    val keyRegister = when (insn) {
                        is BuilderInstruction35c -> when (insn.registerCount) { 1 -> insn.registerC else -> insn.registerD }
                        is BuilderInstruction3rc -> insn.startRegister + 1
                        else -> continue
                    }
                    var keyValue: String? = null
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_STRING) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != keyRegister) continue
                        keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                        break
                    }
                    if (keyValue != "pointer_location") continue
                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        method.replaceInstruction(index, "const/4 v$resultRegister, $target")
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) logger.info("Faked pointer location at $patched call site(s)")
        else logger.warning("No pointer_location reads found. No changes applied.")
    }
}
