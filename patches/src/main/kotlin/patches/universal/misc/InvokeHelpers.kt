package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode

private val LOAD_OPCODES = setOf(
    Opcode.CONST, Opcode.CONST_4, Opcode.CONST_16, Opcode.CONST_WIDE,
    Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO,
    Opcode.MOVE, Opcode.MOVE_OBJECT, Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT,
    Opcode.MOVE_RESULT_WIDE, Opcode.IGET, Opcode.IGET_OBJECT, Opcode.IGET_WIDE,
    Opcode.SGET, Opcode.SGET_OBJECT, Opcode.NEW_ARRAY, Opcode.AGET, Opcode.AGET_OBJECT,
    Opcode.AGET_WIDE,
)

/**
 * Replaces every `invoke-*` to a void [methods] of [targetClass] with a `nop`,
 * dropping the call. Only targets methods whose return type is `V`.
 *
 * @return number of call sites neutralized.
 */
internal fun BytecodePatchContext.noOpVoidInvoke(
    targetClass: String,
    methods: Set<String>,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name !in methods) continue
                if (ref.returnType != "V") continue
                method.replaceInstruction(index, "nop")
                patched++
            }
        }
    }
    return patched
}

/**
 * Forces the [paramIndex]-th real parameter (0-based, excluding `this`) of every
 * call to [methods] of [targetClass] to `null` by replacing the immediately
 * preceding instruction that loads that register with `const/4 vR, 0x0`.
 *
 * Used to silence e.g. `Notification.Builder.setSound(Uri)` by nulling the Uri.
 *
 * @return number of call sites patched.
 */
internal fun BytecodePatchContext.forceNullParam(
    targetClass: String,
    methods: Set<String>,
    paramIndex: Int = 0,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name !in methods) continue

                val nullReg = when (insn) {
                    is BuilderInstruction35c -> insn.registerC + 1 + paramIndex
                    is BuilderInstruction3rc -> insn.startRegister + 1 + paramIndex
                    else -> continue
                }

                val prev = instructions.getOrNull(index - 1)
                if (prev is OneRegisterInstruction &&
                    prev.registerA == nullReg &&
                    prev.opcode in LOAD_OPCODES
                ) {
                    method.replaceInstruction(index - 1, "const/4 v$nullReg, 0x0")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Replaces every array-returning getter [methods] of [targetClass] with an empty
 * array of [elementType] (e.g. `"Landroid/accounts/Account;"`), keeping the
 * following `move-result-object` valid. Used for privacy-by-denial (return
 * nothing instead of the real collection).
 *
 * Emits, in place of the invoke:
 *   const/4 vR, 0x0
 *   new-array vR, vR, [<elementType>   ; (size 0 -> empty array)
 * and rewrites the original `move-result-object vR` to `nop`.
 *
 * @return number of call sites patched.
 */
internal fun BytecodePatchContext.replaceArrayGetterWithEmpty(
    targetClass: String,
    methods: Set<String>,
    elementType: String,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()
            for ((index, insn) in instructions.withIndex()) {
                if (insn !is ReferenceInstruction) continue
                val ref = insn.reference as? MethodReference ?: continue
                if (ref.definingClass != targetClass) continue
                if (ref.name !in methods) continue
                if (!ref.returnType.endsWith("]")) continue

                val next = instructions.getOrNull(index + 1)
                if (next !is OneRegisterInstruction || next.opcode != Opcode.MOVE_RESULT_OBJECT) continue
                val resultReg = next.registerA

                method.replaceInstruction(index, "const/4 v$resultReg, 0x0")
                method.addInstruction(
                    index + 1,
                    "new-array v$resultReg, v$resultReg, [$elementType",
                )
                method.replaceInstruction(index + 2, "nop")
                patched++
            }
        }
    }
    return patched
}
