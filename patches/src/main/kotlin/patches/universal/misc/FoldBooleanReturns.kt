package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Forces every matching boolean-returning method call to a constant by folding
 * the invoke and its following move-result into a single const/4.
 *
 * [targets] maps definingClass -> set of method names -> smali constant ("0x0"/"0x1").
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldBooleanReturns(
    targets: Map<String, Map<String, String>>,
): Int {
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

                val value = targets[reference.definingClass]?.get(reference.name) ?: continue
                if (reference.returnType != "Z") continue

                val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    method.replaceInstruction(index, "const/4 v${next.registerA}, $value")
                    method.replaceInstruction(index + 1, "nop")
                } else {
                    method.replaceInstruction(index, "nop")
                }
                patched++
            }
        }
    }
    return patched
}
