package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
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
val forceChargingStatusPatch = bytecodePatch(
    name = "Force Charging Status",
    description = "Reports a chosen charging status through BatteryManager.getIntProperty for the status property, so apps that gate features behind a charging state stop doing so.",
    default = false,
) {
    val status by stringOption(
        title = "Battery status",
        default = "Charging",
        key = "chargingStatus",
        description = "Status to report: Charging, Discharging, Full, Not Charging.",
        values = linkedMapOf(
            "Charging" to "2",
            "Discharging" to "3",
            "Not Charging" to "4",
            "Full" to "5",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = status?.toIntOrNull() ?: 2
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

                    // Only spoof the status property (BATTERY_PROPERTY_STATUS = 1).
                    var isStatus = false
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral ?: continue
                        if (lit == 1) isStatus = true
                        break
                    }
                    if (!isStatus) continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        method.replaceInstruction(index, if (resultRegister <= 0xff) {
                            "const/4 v$resultRegister, $target"
                        } else {
                            "const/16 v$resultRegister, $target"
                        })
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Forced charging status at $patched call site(s)")
        } else {
            logger.warning("No battery status property reads found. No changes applied.")
        }
    }
}
