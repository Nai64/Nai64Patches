package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private const val BUILD_CLASS = "Landroid/os/Build;"

private fun escapeSmali(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

@Suppress("unused")
val spoofCpuArchitecturePatch = bytecodePatch(
    name = "Spoof CPU Architecture",
    description =
        "Replaces reads of Build.CPU_ABI and Build.CPU_ABI2 with a custom value. Leave empty " +
            "to keep the original",
    default = false,
) {
    val abi by stringOption(
        title = "CPU ABI",
        default = "",
        values = linkedMapOf(
            "Keep original" to "",
            "arm64-v8a" to "arm64-v8a",
            "armeabi-v7a" to "armeabi-v7a",
            "x86_64" to "x86_64",
            "x86" to "x86",
        ),
        key = "spoofCpuAbi",
        description = "Reported CPU architecture. Only affects the legacy CPU_ABI fields.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = abi.orEmpty().trim()
        if (value.isEmpty()) {
            logger.warning("No CPU architecture configured. No changes applied.")
            return@execute
        }

        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val implementation = method.implementation ?: continue
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val field =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                            ?: continue
                    if (field.definingClass != BUILD_CLASS) continue
                    if (field.name != "CPU_ABI" && field.name != "CPU_ABI2") continue

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        method.replaceInstruction(
                            index,
                            "const-string v${next.registerA}, \"${escapeSmali(value)}\"",
                        )
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Spoofed $patched CPU architecture read(s)")
        } else {
            logger.warning("No Build.CPU_ABI reads found. No changes applied.")
        }
    }
}
