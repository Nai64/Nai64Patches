package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

@Suppress("unused")
val fakeBatteryWhitelistPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes PowerManager.isIgnoringBatteryOptimizations always return true so apps stop " +
            "nagging about battery optimization exemptions",
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
                val instructions = implementation.instructions.toList()
                for ((index, instruction) in instructions.withIndex()) {
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: continue

                    if (reference.definingClass != "Landroid/os/PowerManager;" ||
                        reference.name != "isIgnoringBatteryOptimizations" ||
                        reference.returnType != "Z"
                    ) {
                        continue
                    }

                    val next = instructions.getOrNull(index + 1) as? OneRegisterInstruction
                    if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                        method.replaceInstruction(index, "const/4 v${next.registerA}, 0x1")
                        method.replaceInstruction(index + 1, "nop")
                    } else {
                        method.replaceInstruction(index, "nop")
                    }
                    patched++
                }
            }
        }

        if (patched > 0) {
            logger.info("Forced $patched battery whitelist check(s) to true")
        } else {
            logger.warning("No battery optimization checks found. No changes applied.")
        }
    }
}
