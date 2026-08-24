package patches.universal.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private const val FLAG_SECURE = 0x2000

private val FLAG_TARGET_METHODS = setOf(
    "Landroid/app/Activity;->addFlags(I)V",
    "Landroid/app/Activity;->setFlags(II)V",
    "Landroid/view/Window;->addFlags(I)V",
    "Landroid/view/Window;->setFlags(II)V",
)

private fun lastRegisterOf(invoke: Instruction): Int? = when (invoke) {
    is RegisterRangeInstruction -> invoke.startRegister + invoke.registerCount - 1
    is FiveRegisterInstruction -> when (invoke.registerCount) {
        1 -> invoke.registerC
        2 -> invoke.registerD
        3 -> invoke.registerE
        4 -> invoke.registerF
        else -> invoke.registerG
    }

    else -> null
}

private data class FlagSecureMatch(
    val classDef: ClassDef,
    val methodName: String,
    val methodParameters: List<String>,
    val methodReturnType: String,
    val index: Int,
    val register: Int,
    val replacement: String,
)

@Suppress("unused")
val allowScreenshotsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Remove the screenshot-blocking window flag",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val matches = mutableListOf<FlagSecureMatch>()

        classDefForEach { classDef ->
            for (method in classDef.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                var fixed = false

                for ((index, insn) in instructions.withIndex()) {
                    if (fixed) break
                    if (insn !is NarrowLiteralInstruction) continue
                    if ((insn.narrowLiteral and FLAG_SECURE) == 0) continue

                    val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue

                    // Look ahead for a setFlags/addFlags call that consumes
                    // this register as its last (flags) argument.
                    var lookahead = 0
                    for (k in index + 1 until instructions.size) {
                        if (lookahead >= 6) break
                        val candidate = instructions[k]
                        if (candidate.opcode == Opcode.NOP) continue
                        lookahead++

                        val reference = (candidate as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        val descriptor = reference.definingClass +
                            "->" + reference.name + "(" +
                            reference.parameterTypes.joinToString("") + ")" +
                            reference.returnType
                        if (descriptor !in FLAG_TARGET_METHODS) continue
                        if (lastRegisterOf(candidate) != reg) continue

                        val newValue = (insn.narrowLiteral and FLAG_SECURE.inv()).toUInt()
                        val newSmali = "const v$reg, 0x${newValue.toString(16).uppercase()}"
                        matches += FlagSecureMatch(
                            classDef = classDef,
                            methodName = method.name,
                            methodParameters = method.parameterTypes.map { it.toString() },
                            methodReturnType = method.returnType,
                            index = index + 1,
                            register = reg,
                            replacement = newSmali,
                        )
                        fixed = true
                        break
                    }
                }
            }
        }

        var patched = 0
        for (match in matches) {
            val mutableClass = mutableClassDefBy(match.classDef)
            val mutableMethod = mutableClass.methods.firstOrNull {
                it.name == match.methodName &&
                    it.parameterTypes == match.methodParameters &&
                    it.returnType == match.methodReturnType
            } ?: continue

            mutableMethod.addInstructions(match.index, match.replacement)
            logger.info("Removed FLAG_SECURE in ${match.classDef.type} -> ${match.methodName}")
            patched++
        }

        if (patched == 0) {
            logger.warning("No FLAG_SECURE usages found. No changes applied.")
        } else {
            logger.info("Removed FLAG_SECURE from $patched method(s)")
        }
    }
}
