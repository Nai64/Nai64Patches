package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val forceBatteryTemperaturePatch = bytecodePatch(
    name = "Force Battery Temperature",
    description = "Reports a chosen battery temperature through BatteryManager.getIntProperty for the temperature property, so apps that throttle or warn on a hot device stop doing so.",
    default = false,
) {
    val temperature by intOption(
        title = "Temperature (0.1C)",
        default = 250,
        key = "batteryTemperature",
        description = "Temperature in tenths of a degree Celsius: 200 (20C), 250 (25C), 300 (30C), 350 (35C).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val targetTemp = temperature ?: 250
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                // Snapshot; one-for-one replacements keep indices valid.
                val instructions: List<Instruction> = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue
                    if (reference.definingClass != "Landroid/os/BatteryManager;") continue
                    if (reference.name != "getIntProperty") continue
                    if (reference.returnType != "I") continue

                    // Property id is the second invoke register (after `this`).
                    val argRegister = when (instruction) {
                        is BuilderInstruction35c -> when (instruction.registerCount) {
                            1 -> instruction.registerC
                            else -> instruction.registerD
                        }
                        is BuilderInstruction3rc -> instruction.startRegister + 1
                        else -> continue
                    }

                    // Only spoof the temperature property (BATTERY_PROPERTY_TEMPERATURE = 2).
                    var isTemperature = false
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral ?: continue
                        if (lit == 2) isTemperature = true
                        break
                    }
                    if (!isTemperature) continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        method.replaceInstruction(index, if (resultRegister <= 0xff) {
                            "const/16 v$resultRegister, $targetTemp"
                        } else {
                            "const v$resultRegister, $targetTemp"
                        })
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Forced battery temperature at $patched call site(s)")
        } else {
            logger.warning("No battery temperature reads found. No changes applied.")
        }
    }
}
