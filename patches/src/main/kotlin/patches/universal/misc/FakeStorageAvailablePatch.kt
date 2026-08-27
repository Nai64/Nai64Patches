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
val fakeStorageAvailablePatch = bytecodePatch(
    name = "Fake Storage Available",
    description = "Reports a chosen amount of free storage through StatFs so apps that refuse to run or nag when storage is low stop doing so.",
    default = false,
) {
    val storageGb by intOption(
        title = "Storage (GB)",
        default = 128,
        key = "storageGb",
        description = "Amount of free storage in GB (16-1024): 64, 128, 256, 512.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val gb = (storageGb ?: 128).toLong().coerceIn(1, 1024)
        val bytes = gb * 1024L * 1024L * 1024L
        val hex = "0x" + java.lang.Long.toHexString(bytes)
        val targets = mapOf(
            "getAvailableBytes" to true,
            "getFreeBytes" to true,
            "getTotalBytes" to true,
        )
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
                    if (reference.definingClass != "Landroid/os/StatFs;") continue
                    if (reference.name !in targets) continue
                    if (reference.returnType != "J") continue

                    val next = instructions.getOrNull(index + 1)
                    if (next != null && next.opcode == Opcode.MOVE_RESULT_WIDE) {
                        val resultRegister = (next as OneRegisterInstruction).registerA
                        val replacement = if (resultRegister <= 0xff) {
                            "const-wide/32 v$resultRegister, $hex"
                        } else {
                            "const-wide v$resultRegister, $hex"
                        }
                        method.replaceInstruction(index, replacement)
                        method.replaceInstruction(index + 1, "nop")
                        patched++
                    }
                }
            }
        }
        if (patched > 0) {
            logger.info("Faked storage available at $patched call site(s)")
        } else {
            logger.warning("No StatFs byte getters found. No changes applied.")
        }
    }
}
