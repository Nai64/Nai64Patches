package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val disableLogcatLoggingPatch = bytecodePatch(
    name = "Disable Logcat Logging",
    description = "No-ops android.util.Log calls so the app stops spamming logcat.",
    default = false,
) {
    val level by stringOption(
        title = "Log level",
        default = "All",
        key = "logLevel",
        description = "Which levels to disable.",
        values = linkedMapOf(
            "All" to "All",
            "Debug/Verbose" to "Debug",
            "All except errors" to "NoError",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val selected = level ?: "All"
        val targets = when (selected) {
            "Debug" -> setOf("d", "v")
            "NoError" -> setOf("d", "v", "i", "w")
            else -> setOf("d", "v", "i", "w", "e", "println")
        }
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != "Landroid/util/Log;") continue
                    if (ref.name !in targets) continue
                    // Log methods return I (or void for some), handle both
                    val next = instructions.getOrNull(index + 1)
                    if (ref.returnType == "I" && next != null && next.opcode == Opcode.MOVE_RESULT) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val constInstr = if (resultRegister <= 0xf) "const/4 v$resultRegister, 0x0" else "const/16 v$resultRegister, 0x0"
                        method.replaceInstruction(index, constInstr)
                        method.replaceInstruction(index + 1, "nop")
                    } else if (next != null && (next.opcode == Opcode.MOVE_RESULT_OBJECT || next.opcode == Opcode.MOVE_RESULT_WIDE)) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val constInstr = if (next.opcode == Opcode.MOVE_RESULT_WIDE) {
                            if (resultRegister <= 0xff) "const-wide v$resultRegister, 0x0" else "const-wide v$resultRegister, 0x0"
                        } else {
                            if (resultRegister <= 0xff) "const/4 v$resultRegister, 0x0" else "const/16 v$resultRegister, 0x0"
                        }
                        method.replaceInstruction(index, constInstr)
                        method.replaceInstruction(index + 1, "nop")
                    } else {
                        method.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }
        if (patched > 0) logger.info("Disabled $patched Log call(s) ($selected)")
        else logger.warning("No Log calls found. No changes applied.")
    }
}
