package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
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
val fakeFontScalePatch = bytecodePatch(
    name = "Fake Font Scale",
    description = "Reports a chosen font scale through Settings.System so apps that restrict features based on font size stop doing so.",
    default = false,
) {
    val fontScale by intOption(
        title = "Font scale (×100)",
        default = 100,
        key = "fontScale",
        description = "Font scale as percentage: 100 = normal (1.0×), 85 = small (0.85×), 115 = large (1.15×), 130 = largest (1.3×), 150 = extra large (1.5×).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val scale = (fontScale ?: 100) / 100.0f
        // Compute IEEE 754 bits; const/high16 uses only the top 16 bits,
        // bottom 16 are zeroed  -  acceptable precision loss for font scaling.
        val floatBits = java.lang.Float.floatToRawIntBits(scale)
        val hex = "0x" + Integer.toHexString(floatBits)
        logger.info("Using font scale $scale ($hex)")

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val instructions: List<Instruction> = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (reference.definingClass != "Landroid/provider/Settings\$System;") continue
                    if (reference.name != "getFloat") continue
                    if (reference.returnType != "F") continue
                    val params = reference.parameterTypes
                    if (params.size < 2) continue
                    if (params[0] != "Landroid/content/ContentResolver;") continue
                    if (params[1] != "Ljava/lang/String;") continue

                    val keyRegister = when (instruction) {
                        is BuilderInstruction35c -> when (instruction.registerCount) {
                            1 -> instruction.registerC
                            else -> instruction.registerD
                        }
                        is BuilderInstruction3rc -> instruction.startRegister + 1
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
                    if (keyValue != "font_scale") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        method.replaceInstruction(index, "const/high16 v$resultRegister, $hex")
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Faked font scale at $patched call site(s)")
        } else {
            logger.warning("No Settings.System font_scale reads found. No changes applied.")
        }
    }
}
