package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val enableUnrestrictedBackgroundWorkPatch = bytecodePatch(
    name = "Enable Unrestricted Background Work",
    description = "Makes JobScheduler/WorkManager jobs ignore idle and not require charging so background work runs even in Doze.",
    default = false,
) {
    val unrestricted by booleanOption(
        title = "Unrestrict",
        default = true,
        key = "unrestrictedBackgroundWork",
        description = "If true, clears idle/charging constraints.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        if (unrestricted != true) {
            logger.info("Unrestrict disabled, skipping.")
            return@execute
        }
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val isJobIdle = ref.definingClass == "Landroid/app/job/JobInfo\$Builder;" && ref.name == "setRequiresDeviceIdle" && ref.returnType == "Landroid/app/job/JobInfo\$Builder;"
                    val isJobCharging = ref.definingClass == "Landroid/app/job/JobInfo\$Builder;" && ref.name == "setRequiresCharging" && ref.returnType == "Landroid/app/job/JobInfo\$Builder;"
                    val isWorkIdle = ref.definingClass == "Landroidx/work/WorkRequest\$Builder;" && ref.name == "setRequiresDeviceIdle" && ref.parameterTypes.size == 1
                    if (!isJobIdle && !isJobCharging && !isWorkIdle) continue

                    // Find boolean arg register (second reg for instance method with 1 bool arg)
                    val argRegister = when (insn) {
                        is BuilderInstruction35c -> when (insn.registerCount) {
                            1 -> insn.registerC
                            else -> insn.registerD
                        }
                        is BuilderInstruction3rc -> insn.startRegister + 1
                        else -> continue
                    }
                    // Walk back to find const that loads the boolean
                    for (j in index - 1 downTo 0) {
                        val prev = instructions[j]
                        val opcode = prev.opcode
                        if (opcode != Opcode.CONST_4 && opcode != Opcode.CONST && opcode != Opcode.CONST_16) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != argRegister) continue
                        method.replaceInstruction(j, "const/4 v$reg, 0x0")
                        patched++
                        break
                    }
                }
            }
        }
        if (patched > 0) logger.info("Unrestricted background work at $patched call site(s)")
        else logger.warning("No Job/Work builder call sites found. No changes applied.")
    }
}
