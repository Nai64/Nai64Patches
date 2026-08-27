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
val fakeVibrateWhenRingingPatch = bytecodePatch(
    name = "Fake Vibrate When Ringing",
    description = "Reports a chosen vibrate-when-ringing state through Settings.System so apps that check ringer vibration behavior stop doing so.",
    default = false,
) {
    val enabled by booleanOption(
        title = "Enable vibrate when ringing",
        default = false,
        key = "vibrateWhenRinging",
        description = "Report vibrate-when-ringing as enabled (true) or disabled (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = if (enabled == true) 1 else 0

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
                    if (keyValue != "vibrate_when_ringing") continue

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
        if (patched > 0) {
            logger.info("Faked vibrate-when-ringing at $patched call site(s)")
        } else {
            logger.warning("No Settings.System vibrate_when_ringing reads found. No changes applied.")
        }
    }
}
