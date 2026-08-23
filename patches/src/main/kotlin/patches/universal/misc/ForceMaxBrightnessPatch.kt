package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

/**
 * Forces the system screen brightness setting to maximum (255) by folding
 * Settings.System.getInt(..., "screen_brightness", ...) into a constant.
 */
@Suppress("unused")
val forceMaxBrightnessPatch = bytecodePatch(
    name = "Force Max Brightness",
    description = "Forces the system screen brightness setting to maximum (255) so apps that read it cannot dim or restrict the screen.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
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
                    if (reference.definingClass != "Landroid/provider/Settings\$System;") continue
                    if (reference.name != "getInt") continue
                    if (reference.returnType != "I") continue
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
                    if (keyValue != "screen_brightness") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val const = if (resultRegister <= 0xf) {
                            "const/4 v$resultRegister, 0xff"
                        } else {
                            "const/16 v$resultRegister, 0xff"
                        }
                        method.replaceInstruction(index, const)
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    } else {
                        method.replaceInstruction(index, "nop")
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Forced max brightness at $patched call site(s)")
        } else {
            logger.warning("No Settings.System screen_brightness reads found. No changes applied.")
        }
    }
}
