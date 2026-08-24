package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

/**
 * Reports a full battery (100%) through BatteryManager.getIntProperty for the
 * capacity property (BATTERY_PROPERTY_CAPACITY = 4), so apps that restrict
 * features behind a low battery no longer do so.
 */
@Suppress("unused")
val spoofBatteryLevelPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Reports a full battery (100%) through BatteryManager.getIntProperty for the capacity property, so apps that restrict features on low battery stop doing so.",
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

                    // Only spoof the capacity property (BATTERY_PROPERTY_CAPACITY = 4).
                    var isCapacity = false
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral ?: continue
                        if (lit == 4) isCapacity = true
                        break
                    }
                    if (!isCapacity) continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val const = if (resultRegister <= 0xf) {
                            "const/4 v$resultRegister, 0x64"
                        } else {
                            "const/16 v$resultRegister, 0x64"
                        }
                        method.replaceInstruction(index, const)
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Spoofed battery capacity at $patched call site(s)")
        } else {
            logger.warning("No BatteryManager.getIntProperty(capacity) call sites found. No changes applied.")
        }
    }
}
