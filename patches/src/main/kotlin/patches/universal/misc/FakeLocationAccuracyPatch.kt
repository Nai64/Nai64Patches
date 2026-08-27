package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val fakeLocationAccuracyPatch = bytecodePatch(
    name = "Fake Location Accuracy",
    description = "Reports a chosen location accuracy through Location.getAccuracy() so apps that restrict features based on low accuracy stop doing so.",
    default = false,
) {
    val accuracy by intOption(
        title = "Accuracy (meters)",
        default = 1,
        key = "locationAccuracy",
        description = "Reported location accuracy in meters (1 = very accurate, 10 = moderate, 100 = inaccurate).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val meters = (accuracy ?: 1).toFloat()
        val floatBits = java.lang.Float.floatToRawIntBits(meters)
        val hex = "0x" + Integer.toHexString(floatBits)

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
            logger.info("Faked location accuracy at $patched call site(s)")
        } else {
            logger.warning("No Location.getAccuracy call sites found. No changes applied.")
        }
    }
}
