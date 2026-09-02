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
 * Forces TextView text to be selectable and long-clickable by flipping the
 * boolean argument of setTextIsSelectable(false) / setLongClickable(false)
 * calls to true, so copy-protected text in apps becomes selectable and
 * copyable.
 */
@Suppress("unused")
val allowTextSelectionPatch = bytecodePatch(
    name = "Allow Text Selection",
    description = "Makes protected text selectable and copyable.",
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
                    if (reference.returnType != "V") continue
                    val setters = when (reference.definingClass) {
                        "Landroid/widget/TextView;" ->
                            setOf("setTextIsSelectable", "setLongClickable")
                        else -> continue
                    }
                    if (reference.name !in setters) continue

                    // Boolean arg is the second invoke register (after `this`).
                    val argRegister = when (instruction) {
                        is BuilderInstruction35c -> when (instruction.registerCount) {
                            1 -> instruction.registerC
                            else -> instruction.registerD
                        }
                        is BuilderInstruction3rc -> instruction.startRegister + 1
                        else -> continue
                    }

                    // Find the const that loads the boolean arg and flip false -> true.
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral ?: continue
                        if (lit != 0) {
                            // Already enabled (or a different value); leave it.
                            break
                        }
                        method.replaceInstruction(j, "const/4 v$argRegister, 0x1")
                        patched++
                        break
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Enabled text selection at $patched call site(s)")
        } else {
            logger.warning("No setTextIsSelectable/setLongClickable(false) call sites found. No changes applied.")
        }
    }
}
