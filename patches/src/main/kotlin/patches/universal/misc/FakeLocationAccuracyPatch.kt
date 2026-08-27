package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

/**
 * Folds Location.getAccuracy() into 1.0 so apps that check location accuracy
 * see a very precise value.
 */
@Suppress("unused")
val fakeLocationAccuracyPatch = bytecodePatch(
    name = "Fake Location Accuracy",
    description = "Reports location accuracy as 1.0 meter through Location.getAccuracy() so apps that restrict features based on low accuracy stop doing so.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
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
                    if (reference.definingClass != "Landroid/location/Location;") continue
                    if (reference.name != "getAccuracy") continue
                    if (reference.returnType != "F") continue
                    if (reference.parameterTypes.isNotEmpty()) continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == com.android.tools.smali.dexlib2.Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        // 1.0f in IEEE 754 = 0x3f800000
                        method.replaceInstruction(index, "const/high16 v$resultRegister, 0x3f800000")
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Faked location accuracy at $patched call site(s)")
        } else {
            logger.warning("No Location.getAccuracy call sites found. No changes applied.")
        }
    }
}
