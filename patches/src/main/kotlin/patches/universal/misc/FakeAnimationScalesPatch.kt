package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
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
val fakeAnimationScalesPatch = bytecodePatch(
    name = "Fake Animation Scales",
    description = "Reports animation scales via Settings so apps that check them see a chosen value.",
    default = false,
) {
    val scale by stringOption(
        title = "Scale",
        default = "Off",
        key = "animationScale",
        description = "Scale to report.",
        values = linkedMapOf(
            "Off" to "0.0",
            "Normal" to "1.0",
            "Fast" to "0.5",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = scale ?: "0.0"
        val keys = setOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions: List<Instruction> = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/provider/Settings\$Global;" && ref.definingClass != "Landroid/provider/Settings\$System;") continue
                    if (ref.name != "getFloat") continue
                    if (ref.returnType != "F") continue
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
                    if (keyValue !in keys) continue
                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        // Use const/high16 for 0.0 (0x00000000) and 1.0 (0x3f800000), 0.5 (0x3f000000)
                        val hex = when (target) {
                            "1.0" -> "0x3f800000"
                            "0.5" -> "0x3f000000"
                            else -> "0x0"
                        }
                        method.replaceInstruction(index, "const/high16 v$resultRegister, $hex")
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) logger.info("Faked animation scales at $patched call site(s) -> $target")
        else logger.warning("No animation scale reads found. No changes applied.")
    }
}
