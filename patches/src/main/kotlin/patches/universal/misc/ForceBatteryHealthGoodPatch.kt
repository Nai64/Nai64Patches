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
val forceBatteryHealthGoodPatch = bytecodePatch(
    name = "Force Battery Health Good",
    description = "Reports battery health as good via BatteryManager so apps that check health stop warning.",
    default = false,
) {
    val health by stringOption(
        title = "Health",
        default = "Good",
        key = "batteryHealth",
        description = "Health to report.",
        values = linkedMapOf(
            "Good" to "2",
            "Overheat" to "3",
            "Dead" to "4",
            "Over voltage" to "5",
            "Failure" to "6",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = (health ?: "2").toIntOrNull() ?: 2
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions: List<Instruction> = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/os/BatteryManager;") continue
                    if (ref.name != "getIntProperty") continue
                    if (ref.returnType != "I") continue
                    val argRegister = when (insn) {
                        is BuilderInstruction35c -> when (insn.registerCount) { 1 -> insn.registerC else -> insn.registerD }
                        is BuilderInstruction3rc -> insn.startRegister + 1
                        else -> continue
                    }
                    var isHealth = false
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral ?: continue
                        if (lit == 3) isHealth = true
                        break
                    }
                    if (!isHealth) continue
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
        if (patched > 0) logger.info("Forced battery health at $patched call site(s)")
        else logger.warning("No battery health reads found. No changes applied.")
    }
}
