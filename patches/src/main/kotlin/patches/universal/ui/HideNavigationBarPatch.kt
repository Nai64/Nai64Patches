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

private const val NAV_HIDE_FLAGS = 0x1002

private val NAV_HIDE_TARGET_METHODS = setOf(
    "Landroid/view/View;->setSystemUiVisibility(I)V",
)

private fun lastHideNavRegisterOf(invoke: Instruction): Int? = when (invoke) {
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

private data class HideNavBarMatch(
    val classDef: ClassDef,
    val methodName: String,
    val methodParameters: List<String>,
    val methodReturnType: String,
    val index: Int,
    val register: Int,
    val replacement: String,
)

@Suppress("unused")
val hideNavigationBarPatch = bytecodePatch(
    name = "Hide Navigation Bar",
    description = "Hides the navigation bar for a cleaner fullscreen view",
    default = false,
) {
    category("Interface")
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val matches = mutableListOf<HideNavBarMatch>()

        classDefForEach { classDef ->
            for (method in classDef.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                var fixed = false

                for ((index, insn) in instructions.withIndex()) {
                    if (fixed) break
                    if (insn !is NarrowLiteralInstruction) continue
                    if ((insn.narrowLiteral and NAV_HIDE_FLAGS) == NAV_HIDE_FLAGS) continue

                    val reg = (insn as? OneRegisterInstruction)?.registerA ?: continue

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
                        if (descriptor !in NAV_HIDE_TARGET_METHODS) continue
                        if (lastHideNavRegisterOf(candidate) != reg) continue

                        val newValue = (insn.narrowLiteral or NAV_HIDE_FLAGS).toUInt()
                        val newSmali = "const v$reg, 0x${newValue.toString(16).uppercase()}"
                        matches += HideNavBarMatch(
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
            logger.info("Added navigation-hide flags in ${match.classDef.type} -> ${match.methodName}")
            patched++
        }

        if (patched == 0) {
            logger.warning("No setSystemUiVisibility usages found. No changes applied.")
        } else {
            logger.info("Hid navigation bar in $patched method(s)")
        }
    }
}
